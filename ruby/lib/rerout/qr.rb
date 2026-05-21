# frozen_string_literal: true

require 'erb'
require 'uri'

module Rerout
  module Resources
    # QR helpers.
    class Qr
      # @param client [Rerout::Client]
      def initialize(client)
        @client = client
      end

      # Build the URL the API serves the QR SVG from. Pure — does not hit
      # the network.
      #
      # @param code [String]
      # @param options [Rerout::QrOptions, Hash, nil]
      # @return [String]
      def url(code, options = nil)
        Qr.build_url(base_url: @client.base_url, code: code, options: options)
      end

      # Fetch the QR as an SVG string. Attaches the bearer token.
      #
      # @param code [String]
      # @param options [Rerout::QrOptions, Hash, nil]
      # @return [String] the rendered SVG markup.
      def svg(code, options = nil)
        raise ArgumentError, 'code is required' if code.nil? || code.to_s.empty?

        qopts = Qr.coerce_options(options)
        path = "/v1/links/#{ERB::Util.url_encode(code.to_s)}/qr"
        @client.request(
          method: :get,
          path: path,
          query: qopts.to_query_hash,
          raw: true
        )
      end

      # @api private — also used by callers that just want a URL without a client.
      def self.build_url(base_url:, code:, options: nil)
        raise ArgumentError, 'code is required' if code.nil? || code.to_s.empty?

        qopts = coerce_options(options)
        base = base_url.to_s.sub(%r{/+\z}, '')
        path = "/v1/links/#{ERB::Util.url_encode(code.to_s)}/qr"
        url = "#{base}#{path}"
        query = qopts.to_query_hash
        return url if query.empty?

        "#{url}?#{URI.encode_www_form(query)}"
      end

      # @api private
      def self.coerce_options(options)
        case options
        when nil then QrOptions.new
        when QrOptions then options
        when Hash then QrOptions.new(**options.transform_keys(&:to_sym))
        else
          raise ArgumentError, 'options must be a Rerout::QrOptions, Hash, or nil'
        end
      end
    end
  end
end
