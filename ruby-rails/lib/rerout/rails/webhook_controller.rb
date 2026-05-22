# frozen_string_literal: true

require 'json'
require 'action_controller'
require 'rerout'

require_relative 'events'

module Rerout
  module Rails
    # `ActionController` endpoint that ingests signed Rerout webhooks.
    #
    # The `rerout:install` generator mounts it for you. To wire it manually:
    #
    #   # config/routes.rb
    #   post '/rerout/webhooks', to: 'rerout/rails/webhook#receive'
    #
    # Behaviour:
    #
    # - Verifies the `X-Rerout-Signature` header against the configured
    #   `webhook_secret` using the base SDK's constant-time HMAC check.
    # - `200` — signature valid and the body is a JSON object; the delivery is
    #   dispatched through {Rerout::Rails::Events}.
    # - `401` — signature missing, malformed, stale, or wrong.
    # - `400` — signature valid but the body is not a JSON object.
    #
    # CSRF protection is skipped — webhooks are server-to-server and carry no
    # session cookie. Subscribe to {Rerout::Rails::Events} topics to react to
    # deliveries; do not subclass this controller to add handlers.
    class WebhookController < ActionController::Base
      # Header Rerout signs every delivery with.
      SIGNATURE_HEADER = 'X-Rerout-Signature'

      # Webhooks are unauthenticated server-to-server POSTs — no CSRF token.
      skip_forgery_protection if respond_to?(:skip_forgery_protection)

      # Verify, parse, and dispatch a single webhook delivery.
      #
      # @return [void]
      def receive
        raw_body = read_raw_body
        signature = request.headers[SIGNATURE_HEADER].to_s

        unless signature_valid?(raw_body, signature)
          return render(json: { error: 'invalid signature' }, status: :unauthorized)
        end

        body = parse_object(raw_body)
        if body.nil?
          return render(json: { error: 'body must be a JSON object' },
                        status: :bad_request)
        end

        event = body['event'].is_a?(String) ? body['event'] : ''
        Events.dispatch(event: event, body: body, request: request)

        render json: { received: true, event: event }, status: :ok
      end

      private

      def config
        Rerout::Rails.config
      end

      # Read the exact request body bytes. `request.body` may already have been
      # consumed by Rails' params parsing, so rewind first.
      def read_raw_body
        body = request.body
        body.rewind if body.respond_to?(:rewind)
        body.read.to_s
      end

      def signature_valid?(raw_body, signature)
        Rerout::Webhooks.verify_signature(
          raw_body: raw_body,
          signature_header: signature,
          secret: config.webhook_secret!,
          tolerance_seconds: config.signature_tolerance_seconds
        )
      end

      # Parse `raw_body` and return it only when it is a JSON object. Returns
      # `nil` for non-JSON, JSON arrays, JSON scalars, or an empty body.
      def parse_object(raw_body)
        return nil if raw_body.empty?

        parsed = JSON.parse(raw_body)
        parsed.is_a?(Hash) ? parsed : nil
      rescue JSON::ParserError
        nil
      end
    end
  end
end
