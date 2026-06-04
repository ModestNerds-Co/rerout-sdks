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

      # Create many links in one call via `POST /v1/links/batch`.
      #
      # Each item may be a {Rerout::CreateLinkInput} or a plain Hash. Only the
      # batch-supported fields (`target_url`, `code`, `expires_at`,
      # `domain_hostname`) are forwarded — richer Smart Link fields are not
      # accepted by the batch endpoint. A failed item does not raise; inspect
      # `result.results[i].ok`.
      #
      # @param inputs [Array<Rerout::CreateLinkInput, Hash>]
      # @return [Rerout::Models::BatchCreateLinksResult]
      def create_batch(inputs)
        links = Array(inputs).map { |item| batch_link_hash(item) }
        if links.empty?
          raise Error.new(
            code: 'bad_request',
            message: 'create_batch requires at least one link.',
            status: 0
          )
        end

        response = @client.request(method: :post, path: '/v1/links/batch', body: { 'links' => links })
        Models::BatchCreateLinksResult.from_hash(response)
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
        raise ArgumentError, 'input must be a Rerout::UpdateLinkInput' unless input.is_a?(UpdateLinkInput)
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

      # Reduce a create-link input to the fields the batch endpoint accepts:
      # `target_url` (required), `code`, `expires_at`, `domain_hostname`.
      def batch_link_hash(item)
        source =
          case item
          when CreateLinkInput then item.to_h
          when Hash then InputSerialization.stringify(item)
          else
            raise ArgumentError, 'batch link items must be a Rerout::CreateLinkInput or Hash'
          end

        unless source.key?('target_url') && !source['target_url'].to_s.empty?
          raise ArgumentError, 'each batch link requires a target_url'
        end

        out = { 'target_url' => source['target_url'] }
        %w[code expires_at domain_hostname].each do |key|
          out[key] = source[key] unless source[key].nil?
        end
        out
      end
    end
  end
end
