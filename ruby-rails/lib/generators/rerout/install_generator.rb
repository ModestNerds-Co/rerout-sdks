# frozen_string_literal: true

require 'rails/generators'
require 'rails/generators/base'

module Rerout
  module Generators
    # `rails generate rerout:install`
    #
    # Drops a `config/initializers/rerout.rb` initializer and appends the
    # webhook route to `config/routes.rb` so a fresh app is wired up in one
    # command.
    class InstallGenerator < ::Rails::Generators::Base
      source_root File.expand_path('templates', __dir__)

      desc 'Creates a Rerout initializer and mounts the webhook route.'

      # Copy the commented initializer into the host application.
      #
      # @return [void]
      def create_initializer
        template 'rerout.rb', 'config/initializers/rerout.rb'
      end

      # Append the webhook receiver route to `config/routes.rb`.
      #
      # @return [void]
      def add_webhook_route
        route_line = "post '/rerout/webhooks', " \
                     "to: 'rerout/rails/webhook#receive', as: :rerout_webhook"
        route(route_line)
      end

      # Print next steps.
      #
      # @return [void]
      def print_post_install
        readme 'POST_INSTALL' if behavior == :invoke
      end
    end
  end
end
