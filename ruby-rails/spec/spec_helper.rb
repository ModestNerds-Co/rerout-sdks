# frozen_string_literal: true

require 'rerout/rails'
require 'openssl'
require 'json'

RSpec.configure do |config|
  config.expect_with :rspec do |expectations|
    expectations.include_chain_clauses_in_custom_matcher_descriptions = true
  end

  config.mock_with :rspec do |mocks|
    mocks.verify_partial_doubles = true
  end

  config.shared_context_metadata_behavior = :apply_to_host_groups
  config.disable_monkey_patching!
  config.order = :random
  Kernel.srand config.seed

  # Each example starts from a clean global configuration.
  config.before do
    Rerout::Rails.reset!
    Rerout::Rails.configure do |c|
      c.api_key = 'rrk_test'
      c.webhook_secret = ReroutRailsHelpers::TEST_SECRET
    end
  end
end

# Shared signing/notification helpers for the suite.
module ReroutRailsHelpers
  TEST_SECRET = 'whsec_test_secret'

  # Build a valid `t=,v1=` signature header for `body`.
  #
  # @param body [String]
  # @param secret [String]
  # @param timestamp [Integer]
  # @return [String]
  def sign_body(body, secret: TEST_SECRET, timestamp: Time.now.to_i)
    hex = OpenSSL::HMAC.hexdigest('SHA256', secret, "#{timestamp}.#{body}")
    "t=#{timestamp},v1=#{hex}"
  end

  # Capture every `ActiveSupport::Notifications` event for `topic` raised
  # inside the block.
  #
  # @param topic [String]
  # @return [Array<Hash>] the captured instrumentation payloads.
  def capture_notifications(topic)
    captured = []
    subscriber = ActiveSupport::Notifications.subscribe(topic) do |*args|
      captured << ActiveSupport::Notifications::Event.new(*args).payload
    end
    yield
    captured
  ensure
    ActiveSupport::Notifications.unsubscribe(subscriber)
  end
end

RSpec.configure do |config|
  config.include ReroutRailsHelpers
end
