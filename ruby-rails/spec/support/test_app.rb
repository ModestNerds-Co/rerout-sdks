# frozen_string_literal: true

# A minimal Rails application used to exercise the webhook controller end to
# end. It boots just enough of Rails — routing + ActionController — to send
# real HTTP requests through Rack without a generated app skeleton.

require 'action_controller/railtie'
require 'rerout/rails'
require 'rerout/rails/webhook_controller'

module ReroutRailsTest
  # The host application the webhook controller is mounted into.
  class Application < Rails::Application
    config.eager_load = false
    config.consider_all_requests_local = true
    config.active_support.to_time_preserves_timezone = :zone if
      config.active_support.respond_to?(:to_time_preserves_timezone=)
    config.secret_key_base = 'rerout-rails-test-secret-key-base'
    config.hosts.clear if config.respond_to?(:hosts)
    config.logger = Logger.new(File::NULL)
    config.eager_load_paths = []
  end
end

ReroutRailsTest::Application.initialize!

ReroutRailsTest::Application.routes.draw do
  post '/rerout/webhooks', to: 'rerout/rails/webhook#receive', as: :rerout_webhook
end
