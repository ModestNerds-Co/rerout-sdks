# frozen_string_literal: true

require 'erb'

module Rerout
  module Resources
    # Tag management namespace — list, create, update, and delete the tags that
    # can be attached to links for the project that owns the API key. Reach it
    # via `client.tags`.
    class Tags
      # @param client [Rerout::Client]
      def initialize(client)
        @client = client
      end

      # List the project's tags with their live link counts.
      #
      # @return [Rerout::Models::ListTagsResult]
      def list
        response = @client.request(method: :get, path: '/v1/projects/me/tags')
        Models::ListTagsResult.from_hash(response)
      end

      # Create a tag. `color` is optional; the server validates and defaults it.
      #
      # @param input [Rerout::CreateTagInput, Hash] the request body.
      # @return [Rerout::Models::Tag] the created tag (no `link_count`).
      def create(input)
        body = coerce_create_input(input)
        response = @client.request(method: :post, path: '/v1/projects/me/tags', body: body)
        Models::Tag.from_hash(response)
      end

      # Update a tag's name and/or color. Mirrors `links.update`: omitted fields
      # are left unchanged. There is no client-side empty-payload check — the
      # server rejects a fully empty patch with `400`.
      #
      # @param tag_id [String] the tag id (`tag_…`).
      # @param input [Rerout::UpdateTagInput, Hash] the patch body.
      # @return [Rerout::Models::Tag] the updated tag.
      def update(tag_id, input)
        body = coerce_update_input(input)
        response = @client.request(method: :patch, path: tag_path(tag_id), body: body)
        Models::Tag.from_hash(response)
      end

      # Delete a tag and drop its assignments from all links. Idempotent.
      #
      # @param tag_id [String] the tag id (`tag_…`).
      # @return [Hash] `{ "deleted" => true }`
      def delete(tag_id)
        @client.request(method: :delete, path: tag_path(tag_id))
      end

      private

      def tag_path(tag_id)
        raise ArgumentError, 'tag_id is required' if tag_id.nil? || tag_id.to_s.empty?

        "/v1/projects/me/tags/#{ERB::Util.url_encode(tag_id.to_s)}"
      end

      def coerce_create_input(input)
        case input
        when CreateTagInput then input.to_h
        when Hash then input
        else
          raise ArgumentError, 'input must be a Rerout::CreateTagInput or Hash'
        end
      end

      def coerce_update_input(input)
        case input
        when UpdateTagInput then input.to_h
        when Hash then input
        else
          raise ArgumentError, 'input must be a Rerout::UpdateTagInput or Hash'
        end
      end
    end
  end
end
