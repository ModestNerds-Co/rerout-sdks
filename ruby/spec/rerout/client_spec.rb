# frozen_string_literal: true

require 'spec_helper'

RSpec.describe Rerout::Client do
  describe 'construction' do
    it 'requires an API key' do
      expect { described_class.new(api_key: nil) }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('missing_api_key') }
    end

    it 'rejects a blank API key' do
      expect { described_class.new(api_key: '   ') }
        .to raise_error(Rerout::Error, /API key is required/)
    end

    it 'rejects a non-string API key' do
      expect { described_class.new(api_key: 12_345) }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('missing_api_key') }
    end

    it 'defaults to the production base URL' do
      client = described_class.new(api_key: 'rrk_test')
      expect(client.base_url).to eq('https://api.rerout.co')
    end

    it 'trims a single trailing slash from base_url' do
      client = described_class.new(api_key: 'rrk_test', base_url: 'https://staging.rerout.co/')
      expect(client.base_url).to eq('https://staging.rerout.co')
    end

    it 'trims multiple trailing slashes from base_url' do
      client = described_class.new(api_key: 'rrk_test', base_url: 'https://staging.rerout.co///')
      expect(client.base_url).to eq('https://staging.rerout.co')
    end

    it 'exposes the links namespace' do
      expect(described_class.new(api_key: 'rrk_test').links).to be_a(Rerout::Resources::Links)
    end

    it 'exposes the project namespace' do
      expect(described_class.new(api_key: 'rrk_test').project).to be_a(Rerout::Resources::Project)
    end

    it 'exposes the qr namespace' do
      expect(described_class.new(api_key: 'rrk_test').qr).to be_a(Rerout::Resources::Qr)
    end

    it 'exposes the conversions namespace' do
      expect(described_class.new(api_key: 'rrk_test').conversions).to be_a(Rerout::Resources::Conversions)
    end

    it 'exposes the tags namespace' do
      expect(described_class.new(api_key: 'rrk_test').tags).to be_a(Rerout::Resources::Tags)
    end
  end

  describe 'request transport' do
    it 'sends a bearer auth header on every call' do
      client, recorded = build_client(api_key: 'rrk_secret') do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me',
                                  body: '{"id":"p1","name":"Acme","slug":"acme"}')
      end
      client.project.me
      expect(recorded.first.headers['Authorization']).to eq('Bearer rrk_secret')
    end

    it 'sends an accept: application/json header' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me',
                                  body: '{"id":"p1","name":"Acme","slug":"acme"}')
      end
      client.project.me
      expect(recorded.first.headers['Accept']).to eq('application/json')
    end

    it 'omits content-type when there is no request body' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me',
                                  body: '{"id":"p1","name":"Acme","slug":"acme"}')
      end
      client.project.me
      expect(recorded.first.headers['Content-Type']).to be_nil
    end

    it 'sets content-type: application/json when sending a body' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/links',
                                  body: link_json)
      end
      client.links.create(Rerout::CreateLinkInput.new(target_url: 'https://example.com'))
      expect(recorded.first.headers['Content-Type']).to eq('application/json')
    end

    it 'serializes the request body as JSON' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/links', body: link_json)
      end
      client.links.create(Rerout::CreateLinkInput.new(target_url: 'https://example.com/x'))
      expect(JSON.parse(recorded.first.body)).to eq('target_url' => 'https://example.com/x')
    end

    it 'passes cursor and limit as query params' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links',
                                  body: '{"links":[],"next_cursor":null}')
      end
      client.links.list(cursor: 40, limit: 10)
      query = recorded.first.url.query
      expect(query).to include('cursor=40').and include('limit=10')
    end

    it 'passes days as a query param on stats' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me/stats',
                                  body: project_stats_json)
      end
      client.project.stats(days: 7)
      expect(recorded.first.url.query).to include('days=7')
    end

    it 'attaches a default User-Agent header' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me',
                                  body: '{"id":"p1","name":"Acme","slug":"acme"}')
      end
      client.project.me
      expect(recorded.first.headers['User-Agent']).to eq("rerout-ruby/#{Rerout::VERSION}")
    end

    it 'honours a custom User-Agent header' do
      client, recorded = build_client(user_agent: 'my-app/2.0') do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me',
                                  body: '{"id":"p1","name":"Acme","slug":"acme"}')
      end
      client.project.me
      expect(recorded.first.headers['User-Agent']).to eq('my-app/2.0')
    end

    it 'returns nil for a 204 No Content response' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :delete, path: '/v1/links/x',
                                  status: 204, body: '')
      end
      expect(client.links.delete('x')).to be_nil
    end
  end

  describe 'error handling' do
    it 'preserves a server-supplied error code and message' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/links', status: 422,
                                  body: '{"code":"bad_target_url","message":"target_url is invalid"}')
      end
      expect { client.links.create(Rerout::CreateLinkInput.new(target_url: 'https://x')) }
        .to raise_error(Rerout::Error) do |e|
          expect(e.code).to eq('bad_target_url')
          expect(e.message).to eq('target_url is invalid')
          expect(e.status).to eq(422)
        end
    end

    it 'preserves server path and timestamp fields when supplied' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/x', status: 404,
                                  body: '{"code":"not_found","message":"no link",' \
                                        '"path":"/v1/links/x","timestamp":"2026-05-20T00:00:00Z"}')
      end
      expect { client.links.get('x') }.to raise_error(Rerout::Error) do |e|
        expect(e.path).to eq('/v1/links/x')
        expect(e.timestamp).to eq('2026-05-20T00:00:00Z')
      end
    end

    it 'uses synthetic unauthorized code on a 401 with no body' do
      expect_synthetic_code(401, 'unauthorized')
    end

    it 'uses synthetic forbidden code on a 403 with no body' do
      expect_synthetic_code(403, 'forbidden')
    end

    it 'uses synthetic not_found code on a 404 with no body' do
      expect_synthetic_code(404, 'not_found')
    end

    it 'uses synthetic rate_limited code on a 429 with no body' do
      expect_synthetic_code(429, 'rate_limited')
    end

    it 'uses synthetic server_error code on a 500 with no body' do
      expect_synthetic_code(500, 'server_error')
    end

    it 'uses synthetic server_error code on a 503 with no body' do
      expect_synthetic_code(503, 'server_error')
    end

    it 'uses synthetic client_error code on a 418 with no body' do
      expect_synthetic_code(418, 'client_error')
    end

    it 'falls back to a synthetic code for a non-JSON error body' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me', status: 500,
                                  body: '<html>oops</html>')
      end
      expect { client.project.me }.to raise_error(Rerout::Error) do |e|
        expect(e.code).to eq('server_error')
        expect(e.status).to eq(500)
      end
    end

    it 'maps a connection failure to network_error' do
      stubs = Faraday::Adapter::Test::Stubs.new do |stub|
        stub.get('/v1/projects/me') { raise Faraday::ConnectionFailed, 'dns' }
      end
      client = client_with(stubs)
      expect { client.project.me }.to raise_error(Rerout::Error) do |e|
        expect(e.code).to eq('network_error')
        expect(e.status).to eq(0)
      end
    end

    it 'maps a timeout to the timeout code' do
      stubs = Faraday::Adapter::Test::Stubs.new do |stub|
        stub.get('/v1/projects/me') { raise Faraday::TimeoutError, 'slow' }
      end
      client = client_with(stubs)
      expect { client.project.me }.to raise_error(Rerout::Error) do |e|
        expect(e.code).to eq('timeout')
        expect(e.status).to eq(0)
      end
    end

    it 'raises unexpected_response on a 2xx non-JSON body' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me', status: 200,
                                  body: 'not json at all')
      end
      expect { client.project.me }.to raise_error(Rerout::Error) do |e|
        expect(e.code).to eq('unexpected_response')
        expect(e.status).to eq(200)
      end
    end

    it 'flags rate-limited errors' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me', status: 429, body: '')
      end
      expect { client.project.me }.to raise_error(Rerout::Error, &:rate_limited?)
    end

    it 'flags server errors' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me', status: 502, body: '')
      end
      expect { client.project.me }.to raise_error(Rerout::Error, &:server_error?)
    end
  end

  # ── helpers ───────────────────────────────────────────────────────────────

  def client_with(stubs)
    connection = Faraday.new { |conn| conn.adapter :test, stubs }
    described_class.new(api_key: 'rrk_test', connection: connection)
  end

  def expect_synthetic_code(status, code)
    client, = build_client do |stubs, rec|
      stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me', status: status, body: '')
    end
    expect { client.project.me }.to raise_error(Rerout::Error) do |e|
      expect(e.code).to eq(code)
      expect(e.status).to eq(status)
    end
  end

  def link_json
    JSON.generate(
      'code' => 'abc', 'short_url' => 'https://rerout.co/abc',
      'domain_hostname' => nil, 'target_url' => 'https://example.com',
      'project_id' => 'p1', 'expires_at' => nil, 'is_active' => true,
      'seo_title' => nil, 'seo_description' => nil, 'seo_image_url' => nil,
      'seo_canonical_url' => nil, 'seo_noindex' => true, 'seo_updated_at' => nil,
      'created_at' => 1, 'updated_at' => 1
    )
  end

  def project_stats_json
    JSON.generate(
      'days' => 7, 'total_clicks' => 0, 'qr_scans' => 0, 'daily' => [],
      'countries' => [], 'referrers' => [], 'devices' => [], 'browsers' => [],
      'top_codes' => []
    )
  end
end
