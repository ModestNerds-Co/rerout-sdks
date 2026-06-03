# frozen_string_literal: true

require 'erb'

module Rerout
  module Resources
    # Webhook endpoint management namespace — create, list, delete endpoints
    # for the project that owns the API key.
    #
    # This is distinct from {Rerout::Webhooks}, which verifies *inbound*
    # delivery signatures. Reach it via `client.webhooks`.
    class Webhooks
      # @param client [Rerout::Client]
      def initialize(client)
        @client = client
      end

      # Create a webhook endpoint. The returned `signing_secret` (`whsec_…`) is
      # shown once — persist it to verify future deliveries.
      #
      # @param input [Rerout::CreateWebhookInput, Hash] the request body.
      # @return [Rerout::Models::CreatedWebhook]
      def create(input)
        body = coerce_input(input)
        response = @client.request(method: :post, path: '/v1/projects/me/webhooks', body: body)
        Models::CreatedWebhook.from_hash(response)
      end

      # List webhook endpoints and the event types the server can deliver.
      #
      # @return [Rerout::Models::ListWebhooksResult]
      def list
        response = @client.request(method: :get, path: '/v1/projects/me/webhooks')
        Models::ListWebhooksResult.from_hash(response)
      end

      # Soft-delete an endpoint and abandon its pending deliveries. Idempotent.
      #
      # @param endpoint_id [String] the endpoint id (`wh_…`).
      # @return [Hash] `{ "deleted" => true }`
      def delete(endpoint_id)
        @client.request(method: :delete, path: webhook_path(endpoint_id))
      end

      private

      def webhook_path(endpoint_id)
        raise ArgumentError, 'endpoint_id is required' if endpoint_id.nil? || endpoint_id.to_s.empty?

        "/v1/projects/me/webhooks/#{ERB::Util.url_encode(endpoint_id.to_s)}"
      end

      def coerce_input(input)
        case input
        when CreateWebhookInput then input.to_h
        when Hash then input
        else
          raise ArgumentError, 'input must be a Rerout::CreateWebhookInput or Hash'
        end
      end
    end
  end
end
