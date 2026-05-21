# frozen_string_literal: true

require 'erb'

module Rerout
  module Resources
    # Link operations namespace.
    class Links
      # @param client [Rerout::Client]
      def initialize(client)
        @client = client
      end

      # Create a new short link.
      #
      # @param input [Rerout::CreateLinkInput, Hash] the request body.
      # @return [Rerout::Models::Link]
      def create(input)
        body = coerce_input(input)
        response = @client.request(method: :post, path: '/v1/links', body: body)
        Models::Link.from_hash(response)
      end

      # Paginated list of links.
      #
      # @param cursor [Integer, nil]
      # @param limit [Integer, nil]
      # @return [Rerout::Models::ListLinksResult]
      def list(cursor: nil, limit: nil)
        query = {}
        query['cursor'] = cursor unless cursor.nil?
        query['limit'] = limit unless limit.nil?
        response = @client.request(method: :get, path: '/v1/links', query: query)
        Models::ListLinksResult.from_hash(response)
      end

      # Fetch a single link.
      #
      # @param code [String]
      # @return [Rerout::Models::Link]
      def get(code)
        response = @client.request(method: :get, path: link_path(code))
        Models::Link.from_hash(response)
      end

      # Update a link. Only fields set on `input` are sent.
      #
      # @param code [String]
      # @param input [Rerout::UpdateLinkInput]
      # @return [Rerout::Models::Link]
      def update(code, input)
        unless input.is_a?(UpdateLinkInput)
          raise ArgumentError, 'input must be a Rerout::UpdateLinkInput'
        end
        if input.empty?
          raise Error.new(
            code: 'empty_update',
            message: 'UpdateLinkInput has no fields set; refusing to send empty PATCH.',
            status: 0
          )
        end

        response = @client.request(method: :patch, path: link_path(code), body: input.to_h)
        Models::Link.from_hash(response)
      end

      # Soft-delete a link.
      #
      # @param code [String]
      # @return [Hash] `{ "deleted" => true }`
      def delete(code)
        @client.request(method: :delete, path: link_path(code))
      end

      # Per-link click stats. Defaults to 30 days.
      #
      # @param code [String]
      # @param days [Integer]
      # @return [Rerout::Models::LinkStats]
      def stats(code, days: 30)
        response = @client.request(
          method: :get,
          path: "#{link_path(code)}/stats",
          query: { 'days' => days }
        )
        Models::LinkStats.from_hash(response)
      end

      private

      def link_path(code)
        raise ArgumentError, 'code is required' if code.nil? || code.to_s.empty?

        "/v1/links/#{ERB::Util.url_encode(code.to_s)}"
      end

      def coerce_input(input)
        case input
        when CreateLinkInput then input.to_h
        when Hash then input
        else
          raise ArgumentError, 'input must be a Rerout::CreateLinkInput or Hash'
        end
      end
    end
  end
end
