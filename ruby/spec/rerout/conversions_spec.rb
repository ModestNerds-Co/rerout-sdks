# frozen_string_literal: true

require 'spec_helper'

RSpec.describe Rerout::Resources::Conversions do
  describe '#record' do
    it 'POSTs a minimal body to /v1/conversions and returns a RecordedConversion' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/conversions',
                                  body: '{"recorded":true,"duplicate":false}')
      end
      result = client.conversions.record('clk_123', 'purchase')

      expect(recorded.first.http_method).to eq(:post)
      expect(recorded.first.url.path).to eq('/v1/conversions')
      expect(JSON.parse(recorded.first.body)).to eq(
        'click_id' => 'clk_123', 'event_name' => 'purchase'
      )
      expect(result).to be_a(Rerout::Models::RecordedConversion)
      expect(result.recorded).to be(true)
      expect(result.duplicate).to be(false)
    end

    it 'forwards value_cents and currency when provided' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/conversions',
                                  body: '{"recorded":true,"duplicate":false}')
      end
      client.conversions.record('clk_123', 'purchase', value_cents: 4999, currency: 'USD')
      expect(JSON.parse(recorded.first.body)).to eq(
        'click_id' => 'clk_123', 'event_name' => 'purchase',
        'value_cents' => 4999, 'currency' => 'USD'
      )
    end

    it 'omits nil optional fields' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/conversions',
                                  body: '{"recorded":true,"duplicate":false}')
      end
      client.conversions.record('clk_123', 'signup', value_cents: nil, currency: nil)
      body = JSON.parse(recorded.first.body)
      expect(body).not_to have_key('value_cents')
      expect(body).not_to have_key('currency')
    end

    it 'reports a duplicate' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/conversions',
                                  body: '{"recorded":false,"duplicate":true}')
      end
      result = client.conversions.record('clk_123', 'purchase')
      expect(result.recorded).to be(false)
      expect(result.duplicate).to be(true)
    end

    it 'raises ArgumentError on a blank click_id' do
      client, = build_client
      expect { client.conversions.record('', 'purchase') }
        .to raise_error(ArgumentError, /click_id is required/)
    end

    it 'raises ArgumentError on a blank event_name' do
      client, = build_client
      expect { client.conversions.record('clk_123', '') }
        .to raise_error(ArgumentError, /event_name is required/)
    end

    it 'surfaces a server error' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/conversions', status: 404,
                                  body: '{"code":"click_not_found","message":"no such click"}')
      end
      expect { client.conversions.record('clk_missing', 'purchase') }
        .to raise_error(Rerout::Error) { |e| expect(e.code).to eq('click_not_found') }
    end
  end
end
