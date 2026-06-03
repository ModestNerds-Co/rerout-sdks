# frozen_string_literal: true

module Rerout
  # Request body for `POST /v1/projects/me/webhooks`. `name`, `url`, and
  # `events` are required; `is_active` and `payload_format` are optional and
  # omitted from the payload when not set (server defaults apply).
  class CreateWebhookInput
    attr_reader :name, :url, :events, :is_active, :payload_format

    # @param name [String] required, human-readable label for the endpoint.
    # @param url [String] required, public https:// URL that receives deliveries.
    # @param events [Array<String>] required, non-empty list of event types
    #   to subscribe to (e.g. `link.created`).
    # @param is_active [Boolean, nil] whether the endpoint starts active.
    #   Server default: `true`.
    # @param payload_format [String, nil] payload encoding — `"json"` or
    #   `"slack"`. Server default: `"json"`.
    def initialize(name:, url:, events:, is_active: nil, payload_format: nil)
      raise ArgumentError, 'name is required' if name.nil? || name.to_s.empty?
      raise ArgumentError, 'url is required' if url.nil? || url.to_s.empty?
      if events.nil? || !events.is_a?(Array) || events.empty?
        raise ArgumentError, 'events is required and must be a non-empty Array'
      end

      @name = name
      @url = url
      @events = events
      @is_active = is_active
      @payload_format = payload_format
      freeze
    end

    # Serialize for the wire. Optional fields are only included when set.
    def to_h
      hash = { 'name' => name, 'url' => url, 'events' => events }
      hash['is_active'] = is_active unless is_active.nil?
      hash['payload_format'] = payload_format unless payload_format.nil?
      hash
    end
  end
end
