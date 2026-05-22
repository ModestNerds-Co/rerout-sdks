# frozen_string_literal: true

require 'spec_helper'

RSpec.describe Rerout::Resources::Links do
  let(:link_payload) do
    {
      'code' => 'q4', 'short_url' => 'https://go.brand.com/q4',
      'domain_hostname' => 'go.brand.com', 'target_url' => 'https://example.com/sale',
      'project_id' => 'p1', 'expires_at' => nil, 'is_active' => true,
      'seo_title' => 'Sale', 'seo_description' => nil, 'seo_image_url' => nil,
      'seo_canonical_url' => nil, 'seo_noindex' => false, 'seo_updated_at' => nil,
      'created_at' => 1_700_000_000, 'updated_at' => 1_700_000_000
    }
  end

  describe '#create' do
    it 'POSTs to /v1/links and returns a Link model' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/links',
                                  body: JSON.generate(link_payload))
      end
      link = client.links.create(
        Rerout::CreateLinkInput.new(target_url: 'https://example.com/sale',
                                    domain_hostname: 'go.brand.com', code: 'q4')
      )
      expect(recorded.first.http_method).to eq(:post)
      expect(link).to be_a(Rerout::Models::Link)
      expect(link.code).to eq('q4')
      expect(link.short_url).to eq('https://go.brand.com/q4')
    end

    it 'accepts a plain Hash as input' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/links',
                                  body: JSON.generate(link_payload))
      end
      client.links.create('target_url' => 'https://example.com/sale')
      expect(JSON.parse(recorded.first.body)).to eq('target_url' => 'https://example.com/sale')
    end

    it 'only serializes set CreateLinkInput fields' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/links',
                                  body: JSON.generate(link_payload))
      end
      client.links.create(
        Rerout::CreateLinkInput.new(target_url: 'https://example.com', seo_noindex: true)
      )
      expect(JSON.parse(recorded.first.body).keys).to contain_exactly('target_url', 'seo_noindex')
    end

    it 'rejects an input that is neither a CreateLinkInput nor a Hash' do
      client, = build_client
      expect { client.links.create(42) }.to raise_error(ArgumentError)
    end

    it 'surfaces a server validation error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/links', status: 422,
                                  body: '{"code":"bad_target_url","message":"invalid url"}')
      end
      expect { client.links.create('target_url' => 'ftp://x') }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('bad_target_url') }
    end
  end

  describe '#list' do
    it 'GETs /v1/links and returns a ListLinksResult' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links',
                                  body: JSON.generate('links' => [link_payload],
                                                      'next_cursor' => 99))
      end
      result = client.links.list
      expect(result).to be_a(Rerout::Models::ListLinksResult)
      expect(result.links.first.code).to eq('q4')
      expect(result.next_cursor).to eq(99)
    end

    it 'omits cursor and limit from the query when nil' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links',
                                  body: '{"links":[],"next_cursor":null}')
      end
      client.links.list
      expect(recorded.first.url.query).to be_nil
    end

    it 'surfaces a server error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links', status: 500, body: '')
      end
      expect { client.links.list }.to raise_error(Rerout::Error, &:server_error?)
    end
  end

  describe '#get' do
    it 'GETs /v1/links/:code and returns a Link' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/q4',
                                  body: JSON.generate(link_payload))
      end
      link = client.links.get('q4')
      expect(recorded.first.url.path).to eq('/v1/links/q4')
      expect(link.code).to eq('q4')
    end

    it 'raises ArgumentError on a blank code' do
      client, = build_client
      expect { client.links.get('') }.to raise_error(ArgumentError, /code is required/)
    end

    it 'surfaces a not_found error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/missing', status: 404,
                                  body: '{"code":"not_found","message":"gone"}')
      end
      expect { client.links.get('missing') }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('not_found') }
    end
  end

  describe '#update' do
    it 'PATCHes /v1/links/:code with the set fields only' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/links/q4',
                                  body: JSON.generate(link_payload))
      end
      client.links.update('q4', Rerout::UpdateLinkInput.new(target_url: 'https://new.example.com'))
      expect(recorded.first.http_method).to eq(:patch)
      expect(JSON.parse(recorded.first.body)).to eq('target_url' => 'https://new.example.com')
    end

    it 'serializes Rerout::CLEAR as a JSON null' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/links/q4',
                                  body: JSON.generate(link_payload))
      end
      client.links.update('q4', Rerout::UpdateLinkInput.new(expires_at: Rerout::CLEAR))
      expect(recorded.first.body).to eq('{"expires_at":null}')
    end

    it 'refuses an empty update without hitting the API' do
      client, recorded = build_client
      expect { client.links.update('q4', Rerout::UpdateLinkInput.new) }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('empty_update') }
      expect(recorded).to be_empty
    end

    it 'rejects a non-UpdateLinkInput argument' do
      client, = build_client
      expect { client.links.update('q4', { target_url: 'x' }) }
        .to raise_error(ArgumentError, /UpdateLinkInput/)
    end

    it 'surfaces a server error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/links/q4', status: 409,
                                  body: '{"code":"conflict","message":"locked"}')
      end
      expect { client.links.update('q4', Rerout::UpdateLinkInput.new(is_active: false)) }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('conflict') }
    end
  end

  describe '#delete' do
    it 'DELETEs /v1/links/:code and returns the parsed body' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :delete, path: '/v1/links/q4',
                                  body: '{"deleted":true}')
      end
      result = client.links.delete('q4')
      expect(recorded.first.http_method).to eq(:delete)
      expect(result).to eq('deleted' => true)
    end

    it 'surfaces a not_found error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :delete, path: '/v1/links/missing', status: 404,
                                  body: '{"code":"not_found","message":"gone"}')
      end
      expect { client.links.delete('missing') }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('not_found') }
    end
  end

  describe '#stats' do
    let(:stats_payload) do
      {
        'code' => 'q4', 'days' => 30, 'total_clicks' => 12, 'qr_scans' => 3,
        'countries' => [{ 'value' => 'ZA', 'clicks' => 9 }],
        'referrers' => [{ 'value' => 'twitter.com', 'clicks' => 4 }]
      }
    end

    it 'GETs /v1/links/:code/stats with the default 30 day window' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/q4/stats',
                                  body: JSON.generate(stats_payload))
      end
      stats = client.links.stats('q4')
      expect(recorded.first.url.query).to include('days=30')
      expect(stats).to be_a(Rerout::Models::LinkStats)
      expect(stats.total_clicks).to eq(12)
      expect(stats.countries.first.value).to eq('ZA')
    end

    it 'passes a custom days window' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/q4/stats',
                                  body: JSON.generate(stats_payload))
      end
      client.links.stats('q4', days: 7)
      expect(recorded.first.url.query).to include('days=7')
    end

    it 'surfaces a not_found error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/missing/stats', status: 404,
                                  body: '{"code":"not_found","message":"gone"}')
      end
      expect { client.links.stats('missing') }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('not_found') }
    end
  end

  describe 'URL encoding edge cases' do
    {
      'hello world' => '/v1/links/hello%20world',
      'a+b' => '/v1/links/a%2Bb',
      'café' => '/v1/links/caf%C3%A9',
      'go/promo' => '/v1/links/go%2Fpromo'
    }.each do |raw_code, encoded_path|
      it "encodes #{raw_code.inspect} into #{encoded_path}" do
        client, recorded = build_client do |stubs, rec|
          stub_endpoint(stubs, rec, method: :get, path: encoded_path,
                                    body: JSON.generate(link_payload))
        end
        client.links.get(raw_code)
        expect(recorded.first.url.path).to eq(encoded_path)
      end
    end
  end

  describe Rerout::Resources::Project do
    let(:project_stats) do
      {
        'days' => 30, 'total_clicks' => 100, 'qr_scans' => 20,
        'daily' => [{ 'day' => 1_700_000_000, 'clicks' => 5, 'qr_scans' => 1 }],
        'countries' => [{ 'value' => 'ZA', 'clicks' => 60 }],
        'referrers' => [], 'devices' => [], 'browsers' => [],
        'top_codes' => [{ 'value' => 'q4', 'clicks' => 40 }]
      }
    end

    it 'fetches aggregate project stats' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me/stats',
                                  body: JSON.generate(project_stats))
      end
      stats = client.project.stats(days: 30)
      expect(recorded.first.url.path).to eq('/v1/projects/me/stats')
      expect(stats).to be_a(Rerout::Models::ProjectStats)
      expect(stats.total_clicks).to eq(100)
      expect(stats.daily.first.clicks).to eq(5)
      expect(stats.top_codes.first.value).to eq('q4')
    end

    it 'fetches the current project identity' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me',
                                  body: '{"id":"p1","name":"Acme","slug":"acme"}')
      end
      project = client.project.me
      expect(project).to be_a(Rerout::Models::Project)
      expect(project.slug).to eq('acme')
    end

    it 'surfaces an unauthorized error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me', status: 401, body: '')
      end
      expect { client.project.me }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('unauthorized') }
    end
  end
end
