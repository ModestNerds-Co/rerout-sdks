# frozen_string_literal: true

require 'spec_helper'

RSpec.describe Rerout::Rails::Configuration do
  subject(:config) { described_class.new }

  describe 'defaults' do
    it 'defaults the timeout to 30 seconds' do
      expect(config.timeout).to eq(30)
    end

    it 'defaults the signature tolerance to the SDK default' do
      expect(config.signature_tolerance_seconds)
        .to eq(Rerout::Webhooks::DEFAULT_TOLERANCE_SECONDS)
    end

    it 'defaults the webhook path' do
      expect(config.webhook_path).to eq('/rerout/webhooks')
    end
  end

  describe '#client' do
    it 'builds a Rerout::Client from the configured api_key' do
      config.api_key = 'rrk_live'
      expect(config.client).to be_a(Rerout::Client)
    end

    it 'caches the client across calls' do
      config.api_key = 'rrk_live'
      first = config.client
      expect(config.client).to be(first)
    end

    it 'honours a custom base_url' do
      config.api_key = 'rrk_live'
      config.base_url = 'https://staging.rerout.co'
      expect(config.client.base_url).to eq('https://staging.rerout.co')
    end

    it 'raises a ConfigurationError when api_key is missing' do
      config.api_key = nil
      expect { config.client }
        .to raise_error(Rerout::Rails::ConfigurationError, /api_key is required/)
    end

    it 'raises a ConfigurationError when api_key is blank' do
      config.api_key = '   '
      expect { config.client }.to raise_error(Rerout::Rails::ConfigurationError)
    end
  end

  describe '#reset_client!' do
    it 'rebuilds the client after a reset' do
      config.api_key = 'rrk_live'
      first = config.client
      config.reset_client!
      expect(config.client).not_to be(first)
    end
  end

  describe '#webhook_secret!' do
    it 'returns the configured secret' do
      config.webhook_secret = 'whsec_abc'
      expect(config.webhook_secret!).to eq('whsec_abc')
    end

    it 'raises a ConfigurationError when the secret is missing' do
      config.webhook_secret = nil
      expect { config.webhook_secret! }
        .to raise_error(Rerout::Rails::ConfigurationError, /webhook_secret is required/)
    end

    it 'raises a ConfigurationError when the secret is blank' do
      config.webhook_secret = ''
      expect { config.webhook_secret! }.to raise_error(Rerout::Rails::ConfigurationError)
    end
  end

  describe 'Rerout::Rails module accessors' do
    it 'exposes a singleton config' do
      first = Rerout::Rails.config
      expect(Rerout::Rails.config).to be(first)
    end

    it 'yields the config to a configure block' do
      Rerout::Rails.configure { |c| c.timeout = 5 }
      expect(Rerout::Rails.config.timeout).to eq(5)
    end

    it 'exposes the shared client' do
      Rerout::Rails.configure { |c| c.api_key = 'rrk_shared' }
      expect(Rerout::Rails.client).to be_a(Rerout::Client)
    end

    it 'drops config on reset!' do
      original = Rerout::Rails.config
      Rerout::Rails.reset!
      expect(Rerout::Rails.config).not_to be(original)
    end
  end
end
