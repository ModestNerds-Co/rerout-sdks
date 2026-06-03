# frozen_string_literal: true

require 'spec_helper'

RSpec.describe Rerout::Resources::Webhooks do
  let(:webhook_payload) do
    {
      'id' => 'wh_abc123', 'project_id' => 'prj_test', 'name' => 'Order events',
      'url' => 'https://example.com/hooks/rerout',
      'events' => ['link.created', 'link.clicked'], 'is_active' => true,
      'payload_format' => 'json', 'created_at' => 1_700_000_000,
      'updated_at' => 1_700_000_000, 'last_delivery_at' => nil,
      'last_success_at' => nil, 'last_failure_at' => nil
    }
  end

  describe '#create' do
    it 'POSTs to /v1/projects/me/webhooks and returns endpoint + signing secret' do
      created = { 'endpoint' => webhook_payload, 'signing_secret' => 'whsec_supersecret' }
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/projects/me/webhooks',
                                  status: 201, body: JSON.generate(created))
      end
      result = client.webhooks.create(
        Rerout::CreateWebhookInput.new(
          name: 'Order events',
          url: 'https://example.com/hooks/rerout',
          events: ['link.created', 'link.clicked']
        )
      )
      expect(recorded.first.http_method).to eq(:post)
      expect(recorded.first.url.path).to eq('/v1/projects/me/webhooks')
      expect(JSON.parse(recorded.first.body)).to eq(
        'name' => 'Order events',
        'url' => 'https://example.com/hooks/rerout',
        'events' => ['link.created', 'link.clicked']
      )
      expect(result).to be_a(Rerout::Models::CreatedWebhook)
      expect(result.signing_secret).to eq('whsec_supersecret')
      expect(result.endpoint).to be_a(Rerout::Models::Webhook)
      expect(result.endpoint.id).to eq('wh_abc123')
    end

    it 'accepts a plain Hash as input' do
      created = { 'endpoint' => webhook_payload, 'signing_secret' => 'whsec_x' }
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/projects/me/webhooks',
                                  status: 201, body: JSON.generate(created))
      end
      client.webhooks.create(
        'name' => 'Order events',
        'url' => 'https://example.com/hooks/rerout',
        'events' => ['link.created']
      )
      expect(JSON.parse(recorded.first.body)).to eq(
        'name' => 'Order events',
        'url' => 'https://example.com/hooks/rerout',
        'events' => ['link.created']
      )
    end

    it 'forwards is_active and payload_format when provided' do
      created = {
        'endpoint' => webhook_payload.merge('is_active' => false, 'payload_format' => 'slack'),
        'signing_secret' => 'whsec_x'
      }
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/projects/me/webhooks',
                                  status: 201, body: JSON.generate(created))
      end
      result = client.webhooks.create(
        Rerout::CreateWebhookInput.new(
          name: 'Slack',
          url: 'https://hooks.slack.com/services/T/B/x',
          events: ['link.created'],
          is_active: false,
          payload_format: 'slack'
        )
      )
      body = JSON.parse(recorded.first.body)
      expect(body['is_active']).to be(false)
      expect(body['payload_format']).to eq('slack')
      expect(result.endpoint.payload_format).to eq('slack')
      expect(result.endpoint.is_active).to be(false)
    end

    it 'omits optional fields from the payload when not set' do
      created = { 'endpoint' => webhook_payload, 'signing_secret' => 'whsec_x' }
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/projects/me/webhooks',
                                  status: 201, body: JSON.generate(created))
      end
      client.webhooks.create(
        Rerout::CreateWebhookInput.new(
          name: 'Order events',
          url: 'https://example.com/hooks/rerout',
          events: ['link.created']
        )
      )
      expect(JSON.parse(recorded.first.body).keys).to contain_exactly('name', 'url', 'events')
    end

    it 'rejects an input that is neither a CreateWebhookInput nor a Hash' do
      client, = build_client
      expect { client.webhooks.create(42) }.to raise_error(ArgumentError)
    end

    it 'raises ArgumentError when required fields are missing' do
      expect { Rerout::CreateWebhookInput.new(name: '', url: 'https://x', events: ['link.created']) }
        .to raise_error(ArgumentError, /name is required/)
      expect { Rerout::CreateWebhookInput.new(name: 'x', url: '', events: ['link.created']) }
        .to raise_error(ArgumentError, /url is required/)
      expect { Rerout::CreateWebhookInput.new(name: 'x', url: 'https://x', events: []) }
        .to raise_error(ArgumentError, /events is required/)
    end

    it 'surfaces a server validation error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/projects/me/webhooks', status: 422,
                                  body: '{"code":"bad_url","message":"url must be https"}')
      end
      expect do
        client.webhooks.create(
          Rerout::CreateWebhookInput.new(name: 'x', url: 'http://x', events: ['link.created'])
        )
      end.to raise_error(Rerout::Error) { |e| expect(e.code).to eq('bad_url') }
    end
  end

  describe '#list' do
    it 'GETs /v1/projects/me/webhooks and returns endpoints + event types' do
      body = {
        'endpoints' => [webhook_payload],
        'event_types' => ['link.created', 'link.clicked', 'domain.verified']
      }
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me/webhooks',
                                  body: JSON.generate(body))
      end
      result = client.webhooks.list
      expect(recorded.first.http_method).to eq(:get)
      expect(recorded.first.url.path).to eq('/v1/projects/me/webhooks')
      expect(result).to be_a(Rerout::Models::ListWebhooksResult)
      expect(result.endpoints).to all(be_a(Rerout::Models::Webhook))
      expect(result.endpoints.first.url).to eq('https://example.com/hooks/rerout')
      expect(result.endpoints.first.events).to eq(['link.created', 'link.clicked'])
      expect(result.event_types).to include('domain.verified')
    end

    it 'defaults endpoints and event_types to empty arrays when keys are absent' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me/webhooks', body: '{}')
      end
      result = client.webhooks.list
      expect(result.endpoints).to eq([])
      expect(result.event_types).to eq([])
    end

    it 'surfaces a server error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/projects/me/webhooks', status: 500, body: '')
      end
      expect { client.webhooks.list }.to raise_error(Rerout::Error, &:server_error?)
    end
  end

  describe '#delete' do
    it 'DELETEs /v1/projects/me/webhooks/:id and returns the parsed body' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :delete, path: '/v1/projects/me/webhooks/wh_abc123',
                                  body: '{"deleted":true}')
      end
      result = client.webhooks.delete('wh_abc123')
      expect(recorded.first.http_method).to eq(:delete)
      expect(recorded.first.url.path).to eq('/v1/projects/me/webhooks/wh_abc123')
      expect(result).to eq('deleted' => true)
    end

    it 'raises ArgumentError on a blank endpoint id' do
      client, = build_client
      expect { client.webhooks.delete('') }.to raise_error(ArgumentError, /endpoint_id is required/)
    end

    it 'URL-encodes the endpoint id' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :delete, path: '/v1/projects/me/webhooks/wh%2Fweird',
                                  body: '{"deleted":true}')
      end
      client.webhooks.delete('wh/weird')
      expect(recorded.first.url.path).to eq('/v1/projects/me/webhooks/wh%2Fweird')
    end

    it 'surfaces a not_found error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :delete, path: '/v1/projects/me/webhooks/missing', status: 404,
                                  body: '{"code":"not_found","message":"gone"}')
      end
      expect { client.webhooks.delete('missing') }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('not_found') }
    end
  end
end
