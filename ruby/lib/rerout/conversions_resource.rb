# frozen_string_literal: true

module Rerout
  module Resources
    # Conversion tracking namespace — record a conversion against a recorded
    # click. Reach it via `client.conversions`.
    class Conversions
      # @param client [Rerout::Client]
      def initialize(client)
        @client = client
      end

      # Record a conversion for a click via `POST /v1/conversions`.
      #
      # @param click_id [String] the id of the click being converted.
      # @param event_name [String] name of the conversion event (e.g. `"purchase"`).
      # @param value_cents [Integer, nil] optional monetary value in the
      #   smallest currency unit (cents). Omitted when nil.
      # @param currency [String, nil] optional ISO-4217 currency code
      #   (e.g. `"USD"`). Omitted when nil.
      # @return [Rerout::Models::RecordedConversion] `recorded` + `duplicate`
      #   flags. Idempotent — a repeat for the same click + event returns
      #   `duplicate: true`.
      def record(click_id, event_name, value_cents: nil, currency: nil)
        raise ArgumentError, 'click_id is required' if click_id.nil? || click_id.to_s.empty?
        raise ArgumentError, 'event_name is required' if event_name.nil? || event_name.to_s.empty?

        body = { 'click_id' => click_id, 'event_name' => event_name }
        body['value_cents'] = value_cents unless value_cents.nil?
        body['currency'] = currency unless currency.nil?

        response = @client.request(method: :post, path: '/v1/conversions', body: body)
        Models::RecordedConversion.from_hash(response)
      end
    end
  end
end
