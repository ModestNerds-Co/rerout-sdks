# frozen_string_literal: true

require 'spec_helper'

RSpec.describe Rerout::Resources::Links, '#create_batch' do
  let(:batch_response) do
    {
      'created' => 2, 'total' => 3,
      'results' => [
        { 'index' => 0, 'ok' => true, 'code' => 'abc' },
        { 'index' => 1, 'ok' => true, 'code' => 'def' },
        { 'index' => 2, 'ok' => false, 'error' => 'bad_target_url' }
      ]
    }
  end

  let(:inputs) do
    [
      Rerout::CreateLinkInput.new(target_url: 'https://example.com/1'),
      Rerout::CreateLinkInput.new(
        target_url: 'https://example.com/2', code: 'def',
        expires_at: 1_800_000_000, domain_hostname: 'go.brand.com'
      ),
      Rerout::CreateLinkInput.new(target_url: 'bad')
    ]
  end

  def batch_client
    build_client do |stubs, rec|
      stub_endpoint(stubs, rec, method: :post, path: '/v1/links/batch',
                                body: JSON.generate(batch_response))
    end
  end

  it 'POSTs the links array to /v1/links/batch' do
    client, recorded = batch_client
    client.links.create_batch(inputs)

    expect(recorded.first.url.path).to eq('/v1/links/batch')
    expect(JSON.parse(recorded.first.body)).to eq(
      'links' => [
        { 'target_url' => 'https://example.com/1' },
        { 'target_url' => 'https://example.com/2', 'code' => 'def',
          'expires_at' => 1_800_000_000, 'domain_hostname' => 'go.brand.com' },
        { 'target_url' => 'bad' }
      ]
    )
  end

  it 'parses per-item results' do
    client, = batch_client
    result = client.links.create_batch(inputs)

    expect(result).to be_a(Rerout::Models::BatchCreateLinksResult)
    expect(result.created).to eq(2)
    expect(result.total).to eq(3)
    expect(result.results.first).to be_a(Rerout::Models::BatchLinkResult)
    expect(result.results.first.code).to eq('abc')
    expect(result.results.last.ok).to be(false)
    expect(result.results.last.error).to eq('bad_target_url')
  end

  it 'accepts plain Hashes' do
    client, recorded = build_client do |stubs, rec|
      stub_endpoint(stubs, rec, method: :post, path: '/v1/links/batch',
                                body: '{"created":1,"total":1,"results":[{"index":0,"ok":true,"code":"abc"}]}')
    end
    client.links.create_batch([{ 'target_url' => 'https://example.com/1' }])
    expect(JSON.parse(recorded.first.body)).to eq(
      'links' => [{ 'target_url' => 'https://example.com/1' }]
    )
  end

  it 'drops non-batch fields like SEO and Smart Link fields' do
    client, recorded = build_client do |stubs, rec|
      stub_endpoint(stubs, rec, method: :post, path: '/v1/links/batch',
                                body: '{"created":1,"total":1,"results":[{"index":0,"ok":true,"code":"abc"}]}')
    end
    client.links.create_batch(
      [
        Rerout::CreateLinkInput.new(
          target_url: 'https://example.com/1', seo_title: 'ignored',
          password: 'ignored', track_conversions: true
        )
      ]
    )
    expect(JSON.parse(recorded.first.body)).to eq(
      'links' => [{ 'target_url' => 'https://example.com/1' }]
    )
  end

  it 'raises without a request when inputs are empty' do
    requested = false
    client, = build_client do |stubs, rec|
      stub_endpoint(stubs, rec, method: :post, path: '/v1/links/batch',
                                body: JSON.generate(batch_response))
      stubs.post('/v1/links/batch') { requested = true }
    end
    expect { client.links.create_batch([]) }
      .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('bad_request') }
    expect(requested).to be(false)
  end

  it 'raises ArgumentError when a batch item lacks target_url' do
    client, = build_client
    expect { client.links.create_batch([{ 'code' => 'x' }]) }
      .to raise_error(ArgumentError, /target_url/)
  end

  it 'surfaces a server error' do
    client, = build_client do |stubs, rec|
      stub_endpoint(stubs, rec, method: :post, path: '/v1/links/batch', status: 429,
                                body: '{"code":"rate_limited","message":"slow down"}')
    end
    expect do
      client.links.create_batch([Rerout::CreateLinkInput.new(target_url: 'https://example.com/1')])
    end.to raise_error(Rerout::Error) { |e| expect(e.code).to eq('rate_limited') }
  end
end
