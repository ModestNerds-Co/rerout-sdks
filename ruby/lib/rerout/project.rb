# frozen_string_literal: true

module Rerout
  module Resources
    # Project-level operations namespace.
    class Project
      # @param client [Rerout::Client]
      def initialize(client)
        @client = client
      end

      # Aggregate stats across every link in the project. Defaults to 30 days.
      #
      # @param days [Integer]
      # @return [Rerout::Models::ProjectStats]
      def stats(days: 30)
        response = @client.request(
          method: :get,
          path: '/v1/projects/me/stats',
          query: { 'days' => days }
        )
        Models::ProjectStats.from_hash(response)
      end

      # Info about the project that owns the current API key.
      #
      # @return [Rerout::Models::Project]
      def me
        response = @client.request(method: :get, path: '/v1/projects/me')
        Models::Project.from_hash(response)
      end
    end
  end
end
