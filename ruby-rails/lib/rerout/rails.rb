# frozen_string_literal: true

require 'rerout'

require_relative 'rails/version'
require_relative 'rails/configuration'
require_relative 'rails/events'

module Rerout
  # Official Rails integration for the Rerout branded-link API.
  #
  # Wraps the base {Rerout} SDK with Rails-native ergonomics:
  #
  # - {Rerout::Rails.client} — a process-wide {Rerout::Client} built from an
  #   initializer.
  # - {Rerout::Rails::WebhookController} — verifies the `X-Rerout-Signature`
  #   header and instruments an `ActiveSupport::Notifications` event per
  #   delivery.
  # - {Rerout::Rails::Events} — the notification topics apps subscribe to.
  #
  # @see https://rerout.co
  # @see https://github.com/ModestNerds-Co/rerout-sdks
  module Rails
    class << self
      # The global {Rerout::Rails::Configuration}.
      #
      # @return [Rerout::Rails::Configuration]
      def config
        @config ||= Configuration.new
      end

      # Yields the configuration for mutation. Call from an initializer.
      #
      #   Rerout::Rails.configure do |c|
      #     c.api_key = ENV.fetch('REROUT_API_KEY')
      #   end
      #
      # @yieldparam config [Rerout::Rails::Configuration]
      # @return [Rerout::Rails::Configuration]
      def configure
        yield(config) if block_given?
        config
      end

      # The shared, lazily-built {Rerout::Client}.
      #
      # @return [Rerout::Client]
      # @raise [Rerout::Rails::ConfigurationError] when `api_key` is unset.
      def client
        config.client
      end

      # Replace the configuration wholesale. Mainly a test seam.
      #
      # @param configuration [Rerout::Rails::Configuration]
      # @return [Rerout::Rails::Configuration]
      attr_writer :config

      # Drop the global configuration and cached client. Test seam.
      #
      # @return [void]
      def reset!
        @config = nil
      end
    end
  end
end

# The railtie pulls in Rails' boot machinery — load it only when Rails itself
# is present so the gem stays usable (config + webhook verification) in plain
# Ruby contexts and test harnesses.
require 'rerout/rails/railtie' if defined?(Rails::Railtie)
