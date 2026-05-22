# frozen_string_literal: true

require 'rerout'

module Rerout
  module Rails
    # Holds the `rerout-rails` configuration and lazily builds a process-wide,
    # cached {Rerout::Client}.
    #
    # Configure it from an initializer (the `rerout:install` generator drops
    # one for you):
    #
    #   Rerout::Rails.configure do |config|
    #     config.api_key = ENV.fetch('REROUT_API_KEY')
    #     config.webhook_secret = ENV.fetch('REROUT_WEBHOOK_SECRET')
    #     # config.base_url = 'https://api.rerout.co'
    #     # config.timeout = 30
    #     # config.signature_tolerance_seconds = 300
    #   end
    #
    # The client is built once on first access and reused — {Rerout::Client}
    # wraps a thread-safe Faraday connection, so sharing one instance is the
    # recommended pattern.
    class Configuration
      # @return [String, nil] project API key (`rrk_…`). Required before
      #   {#client} can be used.
      attr_accessor :api_key

      # @return [String, nil] endpoint signing secret (`whsec_…`). Required
      #   for webhook signature verification.
      attr_accessor :webhook_secret

      # @return [String, nil] override the API base URL.
      attr_accessor :base_url

      # @return [Integer] per-request timeout in seconds. Default 30.
      attr_accessor :timeout

      # @return [Integer] webhook signature timestamp tolerance in seconds.
      #   `0` disables the staleness check. Default 300.
      attr_accessor :signature_tolerance_seconds

      # @return [String, nil] override the SDK `User-Agent` header.
      attr_accessor :user_agent

      # @return [String] path the webhook controller is mounted at when the
      #   generated routes are used. Default `/rerout/webhooks`.
      attr_accessor :webhook_path

      def initialize
        @api_key = ENV.fetch('REROUT_API_KEY', nil)
        @webhook_secret = ENV.fetch('REROUT_WEBHOOK_SECRET', nil)
        @base_url = ENV.fetch('REROUT_BASE_URL', nil)
        @timeout = 30
        @signature_tolerance_seconds = Rerout::Webhooks::DEFAULT_TOLERANCE_SECONDS
        @user_agent = nil
        @webhook_path = '/rerout/webhooks'
        @client = nil
        @client_mutex = Mutex.new
      end

      # The process-wide cached {Rerout::Client}, built from this config.
      #
      # @return [Rerout::Client]
      # @raise [Rerout::Rails::ConfigurationError] when `api_key` is unset.
      def client
        return @client if @client

        @client_mutex.synchronize do
          @client ||= build_client
        end
      end

      # Drop the cached client. The next {#client} call rebuilds it. Useful
      # after changing credentials in tests.
      #
      # @return [void]
      def reset_client!
        @client_mutex.synchronize { @client = nil }
      end

      # The configured webhook signing secret.
      #
      # @return [String]
      # @raise [Rerout::Rails::ConfigurationError] when unset or blank.
      def webhook_secret!
        if webhook_secret.nil? || webhook_secret.to_s.strip.empty?
          raise ConfigurationError,
                'Rerout::Rails.config.webhook_secret is required to verify ' \
                'webhook signatures. Set it in config/initializers/rerout.rb ' \
                'or via the REROUT_WEBHOOK_SECRET environment variable.'
        end
        webhook_secret
      end

      private

      def build_client
        if api_key.nil? || api_key.to_s.strip.empty?
          raise ConfigurationError,
                'Rerout::Rails.config.api_key is required to build a Rerout ' \
                'client. Set it in config/initializers/rerout.rb or via the ' \
                'REROUT_API_KEY environment variable.'
        end

        Rerout::Client.new(
          api_key: api_key,
          base_url: base_url,
          timeout: timeout,
          user_agent: user_agent
        )
      end
    end

    # Raised when `rerout-rails` is missing required configuration.
    class ConfigurationError < StandardError; end
  end
end
