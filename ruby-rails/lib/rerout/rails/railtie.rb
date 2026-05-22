# frozen_string_literal: true

require 'rails/railtie'

module Rerout
  module Rails
    # Hooks `rerout-rails` into the Rails boot process.
    #
    # The railtie keeps the integration zero-config beyond the initializer:
    # the webhook controller and generators are autoloaded by Rails' standard
    # `lib/` eager-loading once the gem is in the `Gemfile`.
    class Railtie < ::Rails::Railtie
      # Namespaced config accessible as `Rails.application.config.rerout`.
      config.rerout = Rerout::Rails.config

      # Make `Rerout::Rails::WebhookController` resolvable by the router
      # without the host app having to require it explicitly.
      initializer 'rerout.rails.controller' do
        require 'rerout/rails/webhook_controller'
      end
    end
  end
end
