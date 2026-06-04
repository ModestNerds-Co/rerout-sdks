# frozen_string_literal: true

require 'spec_helper'

RSpec.describe 'Smart Link fields' do # rubocop:disable RSpec/DescribeClass
  let(:smart_link_payload) do
    {
      'code' => 'q4', 'short_url' => 'https://go.brand.com/q4',
      'domain_hostname' => 'go.brand.com', 'target_url' => 'https://example.com/sale',
      'project_id' => 'p1', 'expires_at' => nil, 'is_active' => true,
      'seo_title' => nil, 'seo_description' => nil, 'seo_image_url' => nil,
      'seo_canonical_url' => nil, 'seo_noindex' => false, 'seo_updated_at' => nil,
      'tags' => [],
      'created_at' => 1_700_000_000, 'updated_at' => 1_700_000_000,
      'password_protected' => true, 'max_clicks' => 100, 'click_count' => 42,
      'track_conversions' => true,
      'routing_rules' => [
        {
          'condition_type' => 'country', 'condition_op' => 'is',
          'condition_value' => 'US', 'target_url' => 'https://example.com/us'
        }
      ],
      'ab_variants' => [
        { 'id' => 1, 'target_url' => 'https://example.com/a', 'weight' => 70 },
        { 'id' => 2, 'target_url' => 'https://example.com/b', 'weight' => 30 }
      ]
    }
  end

  let(:bare_link_payload) do
    {
      'code' => 'q4', 'short_url' => 'https://go.brand.com/q4',
      'domain_hostname' => nil, 'target_url' => 'https://example.com/sale',
      'project_id' => 'p1', 'expires_at' => nil, 'is_active' => true,
      'seo_noindex' => false,
      'created_at' => 1_700_000_000, 'updated_at' => 1_700_000_000
    }
  end

  describe 'Link parsing' do
    def smart_link
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/q4',
                                  body: JSON.generate(smart_link_payload))
      end
      client.links.get('q4')
    end

    it 'parses scalar Smart Link fields' do
      link = smart_link
      expect(link.password_protected).to be(true)
      expect(link.max_clicks).to eq(100)
      expect(link.click_count).to eq(42)
      expect(link.track_conversions).to be(true)
    end

    it 'parses routing rules' do
      rule = smart_link.routing_rules.first
      expect(rule).to be_a(Rerout::Models::RoutingRule)
      expect(rule.condition_type).to eq('country')
      expect(rule.condition_op).to eq('is')
      expect(rule.condition_value).to eq('US')
      expect(rule.target_url).to eq('https://example.com/us')
    end

    it 'parses A/B variants' do
      variant = smart_link.ab_variants.first
      expect(variant).to be_a(Rerout::Models::AbVariant)
      expect(variant.id).to eq(1)
      expect(variant.target_url).to eq('https://example.com/a')
      expect(variant.weight).to eq(70)
    end

    it 'falls back to safe defaults when the new keys are absent' do
      client, = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :get, path: '/v1/links/q4',
                                  body: JSON.generate(bare_link_payload))
      end
      link = client.links.get('q4')
      expect(link.password_protected).to be(false)
      expect(link.max_clicks).to be_nil
      expect(link.click_count).to eq(0)
      expect(link.track_conversions).to be(false)
      expect(link.routing_rules).to eq([])
      expect(link.ab_variants).to eq([])
    end

    it 'defaults AbVariant weight to 1' do
      variant = Rerout::Models::AbVariant.from_hash('target_url' => 'https://example.com/a')
      expect(variant.weight).to eq(1)
      expect(variant.id).to be_nil
    end
  end

  describe 'CreateLinkInput serialization' do
    let(:full_input) do
      Rerout::CreateLinkInput.new(
        target_url: 'https://example.com',
        password: 'hunter2',
        max_clicks: 500,
        track_conversions: true,
        routing_rules: [
          Rerout::Models::RoutingRule.new(
            condition_type: 'device', condition_op: 'is',
            condition_value: 'mobile', target_url: 'https://m.example.com'
          )
        ],
        ab_variants: [
          Rerout::Models::AbVariant.new(target_url: 'https://example.com/a', weight: 2),
          Rerout::Models::AbVariant.new(target_url: 'https://example.com/b')
        ]
      )
    end

    it 'includes scalar Smart Link fields' do
      hash = full_input.to_h
      expect(hash['password']).to eq('hunter2')
      expect(hash['max_clicks']).to eq(500)
      expect(hash['track_conversions']).to be(true)
    end

    it 'serializes routing rules and A/B variants' do
      hash = full_input.to_h
      expect(hash['routing_rules']).to eq(
        [{ 'condition_type' => 'device', 'condition_op' => 'is',
           'condition_value' => 'mobile', 'target_url' => 'https://m.example.com' }]
      )
      expect(hash['ab_variants']).to eq(
        [{ 'target_url' => 'https://example.com/a', 'weight' => 2 },
         { 'target_url' => 'https://example.com/b', 'weight' => 1 }]
      )
    end

    it 'accepts plain Hashes for routing_rules and ab_variants and strips variant id' do
      input = Rerout::CreateLinkInput.new(
        target_url: 'https://example.com',
        routing_rules: [{ condition_type: 'country', condition_op: 'in',
                          condition_value: 'US,CA', target_url: 'https://example.com/na' }],
        ab_variants: [{ id: 99, target_url: 'https://example.com/a' }]
      )
      hash = input.to_h
      expect(hash['routing_rules'].first['condition_op']).to eq('in')
      expect(hash['ab_variants'].first).to eq('target_url' => 'https://example.com/a', 'weight' => 1)
      expect(hash['ab_variants'].first).not_to have_key('id')
    end

    it 'omits Smart Link fields when not set' do
      hash = Rerout::CreateLinkInput.new(target_url: 'https://example.com').to_h
      %w[password max_clicks track_conversions routing_rules ab_variants].each do |key|
        expect(hash).not_to have_key(key)
      end
    end

    it 'sends empty arrays when explicitly provided' do
      hash = Rerout::CreateLinkInput.new(
        target_url: 'https://example.com', routing_rules: [], ab_variants: []
      ).to_h
      expect(hash['routing_rules']).to eq([])
      expect(hash['ab_variants']).to eq([])
    end

    it 'POSTs Smart Link fields on the wire' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :post, path: '/v1/links',
                                  body: JSON.generate(smart_link_payload))
      end
      client.links.create(
        Rerout::CreateLinkInput.new(
          target_url: 'https://example.com', track_conversions: true, max_clicks: 5
        )
      )
      body = JSON.parse(recorded.first.body)
      expect(body['track_conversions']).to be(true)
      expect(body['max_clicks']).to eq(5)
    end
  end

  describe 'UpdateLinkInput serialization' do
    it 'includes Smart Link fields' do
      hash = Rerout::UpdateLinkInput.new(
        password: 'new-secret',
        max_clicks: 10,
        track_conversions: false,
        routing_rules: [
          Rerout::Models::RoutingRule.new(
            condition_type: 'country', condition_op: 'in',
            condition_value: 'US,CA', target_url: 'https://example.com/na'
          )
        ],
        ab_variants: [Rerout::Models::AbVariant.new(target_url: 'https://example.com/x', weight: 5)]
      ).to_h
      expect(hash['password']).to eq('new-secret')
      expect(hash['max_clicks']).to eq(10)
      expect(hash['track_conversions']).to be(false)
      expect(hash['routing_rules'].first['condition_op']).to eq('in')
      expect(hash['ab_variants']).to eq([{ 'target_url' => 'https://example.com/x', 'weight' => 5 }])
    end

    it 'clears password and max_clicks with Rerout::CLEAR' do
      hash = Rerout::UpdateLinkInput.new(password: Rerout::CLEAR, max_clicks: Rerout::CLEAR).to_h
      expect(hash).to eq('password' => nil, 'max_clicks' => nil)
    end

    it 'omits unset Smart Link fields' do
      hash = Rerout::UpdateLinkInput.new(is_active: true).to_h
      expect(hash).to eq('is_active' => true)
    end

    it 'treats an empty routing_rules array as a full clear (not empty?)' do
      input = Rerout::UpdateLinkInput.new(routing_rules: [])
      expect(input.empty?).to be(false)
      expect(input.to_h).to eq('routing_rules' => [])
    end

    it 'PATCHes a full replacement of routing_rules' do
      client, recorded = build_client do |stubs, rec|
        stub_endpoint(stubs, rec, method: :patch, path: '/v1/links/q4',
                                  body: JSON.generate(smart_link_payload))
      end
      client.links.update('q4', Rerout::UpdateLinkInput.new(routing_rules: []))
      expect(JSON.parse(recorded.first.body)).to eq('routing_rules' => [])
    end
  end
end
