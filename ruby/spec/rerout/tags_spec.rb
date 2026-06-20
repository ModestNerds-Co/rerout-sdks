# frozen_string_literal: true

require 'spec_helper'

RSpec.describe Rerout::Resources::Tags do
  let(:tag_payload) { { 'id' => 'tag_abc123', 'name' => 'Spring 2026', 'color' => 'teal' } }
  let(:summary_payload) { tag_payload.merge('link_count' => 4) }

  describe '#list' do
    it 'GETs /v1/projects/me/tags and returns tags with link counts' do
      body = { 'tags' => [summary_payload] }
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me/tags', body: JSON.generate(body))
      end
      result = client.tags.list
      expect(recorded.first.http_method).to eq(:get)
      expect(recorded.first.url.path).to eq('/v1/projects/me/tags')
      expect(result).to be_a(Rerout::Models::ListTagsResult)
      expect(result.tags).to all(be_a(Rerout::Models::TagSummary))
      expect(result.tags.size).to eq(1)
      expect(result.tags.first.to_h).to eq(
        id: 'tag_abc123', name: 'Spring 2026', color: 'teal', link_count: 4
      )
    end

    it 'defaults tags to an empty array when the key is absent' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me/tags', body: '{}')
      end
      expect(client.tags.list.tags).to eq([])
    end

    it 'surfaces a server error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me/tags', status: 500, body: '')
      end
      expect { client.tags.list }.to raise_error(Rerout::Error, &:server_error?)
    end
  end

  describe '#create' do
    it 'POSTs the name and color and returns the created tag' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/projects/me/tags',
                                  status: 201, body: JSON.generate(tag_payload))
      end
      result = client.tags.create(Rerout::CreateTagInput.new(name: 'Spring 2026', color: 'teal'))
      expect(recorded.first.http_method).to eq(:post)
      expect(recorded.first.url.path).to eq('/v1/projects/me/tags')
      expect(JSON.parse(recorded.first.body)).to eq('name' => 'Spring 2026', 'color' => 'teal')
      expect(result).to be_a(Rerout::Models::Tag)
      expect(result.id).to eq('tag_abc123')
    end

    it 'omits color from the payload when not set' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/projects/me/tags',
                                  status: 201, body: JSON.generate(tag_payload))
      end
      client.tags.create(Rerout::CreateTagInput.new(name: 'Spring 2026'))
      expect(JSON.parse(recorded.first.body)).to eq('name' => 'Spring 2026')
    end

    it 'accepts a plain Hash as input' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/projects/me/tags',
                                  status: 201, body: JSON.generate(tag_payload))
      end
      client.tags.create('name' => 'Spring 2026', 'color' => 'teal')
      expect(JSON.parse(recorded.first.body)).to eq('name' => 'Spring 2026', 'color' => 'teal')
    end

    it 'rejects an input that is neither a CreateTagInput nor a Hash' do
      client, = build_client
      expect { client.tags.create(42) }.to raise_error(ArgumentError)
    end

    it 'raises ArgumentError when name is missing' do
      expect { Rerout::CreateTagInput.new(name: '') }.to raise_error(ArgumentError, /name is required/)
    end

    it 'surfaces a server validation error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/projects/me/tags', status: 422,
                                  body: '{"code":"bad_color","message":"unknown color"}')
      end
      expect { client.tags.create(Rerout::CreateTagInput.new(name: 'x', color: 'chartreuse')) }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('bad_color') }
    end
  end

  describe '#update' do
    it 'PATCHes the tag by id and forwards only the provided fields' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/projects/me/tags/tag_abc123',
                                  body: JSON.generate(tag_payload.merge('color' => 'red')))
      end
      result = client.tags.update('tag_abc123', Rerout::UpdateTagInput.new(color: 'red'))
      expect(recorded.first.http_method).to eq(:patch)
      expect(recorded.first.url.path).to eq('/v1/projects/me/tags/tag_abc123')
      expect(JSON.parse(recorded.first.body)).to eq('color' => 'red')
      expect(result).to be_a(Rerout::Models::Tag)
      expect(result.color).to eq('red')
    end

    it 'forwards only the name when only the name is set' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/projects/me/tags/tag_abc123',
                                  body: JSON.generate(tag_payload.merge('name' => 'Renamed')))
      end
      result = client.tags.update('tag_abc123', Rerout::UpdateTagInput.new(name: 'Renamed'))
      expect(JSON.parse(recorded.first.body)).to eq('name' => 'Renamed')
      expect(result.name).to eq('Renamed')
    end

    it 'accepts a plain Hash as input' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/projects/me/tags/tag_abc123',
                                  body: JSON.generate(tag_payload.merge('name' => 'Renamed')))
      end
      client.tags.update('tag_abc123', { 'name' => 'Renamed' })
      expect(JSON.parse(recorded.first.body)).to eq('name' => 'Renamed')
    end

    it 'sends an empty body for an empty patch (server rejects it)' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/projects/me/tags/tag_abc123', status: 400,
                                  body: '{"code":"empty_update","message":"nothing to update"}')
      end
      expect { client.tags.update('tag_abc123', Rerout::UpdateTagInput.new) }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('empty_update') }
      expect(JSON.parse(recorded.first.body)).to eq({})
    end

    it 'rejects an input that is neither an UpdateTagInput nor a Hash' do
      client, = build_client
      expect { client.tags.update('tag_abc123', 42) }.to raise_error(ArgumentError)
    end

    it 'raises ArgumentError on a blank tag id' do
      client, = build_client
      expect { client.tags.update('', Rerout::UpdateTagInput.new(name: 'x')) }
        .to raise_error(ArgumentError, /tag_id is required/)
    end

    it 'URL-encodes the tag id' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/projects/me/tags/tag%2Fweird',
                                  body: JSON.generate(tag_payload))
      end
      client.tags.update('tag/weird', Rerout::UpdateTagInput.new(name: 'x'))
      expect(recorded.first.url.path).to eq('/v1/projects/me/tags/tag%2Fweird')
    end

    it 'surfaces a not_found error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/projects/me/tags/missing', status: 404,
                                  body: '{"code":"not_found","message":"gone"}')
      end
      expect { client.tags.update('missing', Rerout::UpdateTagInput.new(name: 'x')) }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('not_found') }
    end
  end

  describe '#delete' do
    it 'DELETEs /v1/projects/me/tags/:id and returns the parsed body' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :delete, path: '/v1/projects/me/tags/tag_abc123',
                                  body: '{"deleted":true}')
      end
      result = client.tags.delete('tag_abc123')
      expect(recorded.first.http_method).to eq(:delete)
      expect(recorded.first.url.path).to eq('/v1/projects/me/tags/tag_abc123')
      expect(result).to eq('deleted' => true)
    end

    it 'raises ArgumentError on a blank tag id' do
      client, = build_client
      expect { client.tags.delete('') }.to raise_error(ArgumentError, /tag_id is required/)
    end

    it 'URL-encodes the tag id' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :delete, path: '/v1/projects/me/tags/tag%2Fweird',
                                  body: '{"deleted":true}')
      end
      client.tags.delete('tag/weird')
      expect(recorded.first.url.path).to eq('/v1/projects/me/tags/tag%2Fweird')
    end

    it 'surfaces a not_found error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :delete, path: '/v1/projects/me/tags/missing', status: 404,
                                  body: '{"code":"not_found","message":"gone"}')
      end
      expect { client.tags.delete('missing') }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('not_found') }
    end
  end
end
