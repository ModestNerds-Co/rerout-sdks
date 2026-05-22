# frozen_string_literal: true

require 'faraday'
require 'json'

require_relative 'version'
require_relative 'error'
require_relative 'create_link_input'
require_relative 'update_link_input'
require_relative 'qr_options'
require_relative 'webhooks'
require_relative 'models'
require_relative 'links'
require_relative 'project'
require_relative 'qr'

module Rerout
  # Default production API base URL.
  DEFAULT_BASE_URL = 'https://api.rerout.co'

  # Main entry point — construct one of these per project API key and re-use
  # it across requests. Thread-safe so long as the injected Faraday connection
  # is thread-safe (Faraday's default `Net::HTTP` adapter is).
  #
  # @example
  #   rerout = Rerout::Client.new(api_key: ENV.fetch('REROUT_API_KEY'))
  #   link = rerout.links.create(Rerout::CreateLinkInput.new(target_url: 'https://example.com'))
  #   puts link.short_url
  class Client
    # @return [String] resolved base URL with trailing slashes stripped.
    attr_reader :base_url

    # @return [Resources::Links] link namespace.
    attr_reader :links
    # @return [Resources::Project] project namespace.
    attr_reader :project
    # @return [Resources::Qr] QR namespace.
    attr_reader :qr

    # @param api_key [String] project API key (`rrk_…`). Required.
    # @param base_url [String, nil] override base URL. Defaults to `https://api.rerout.co`.
    # @param connection [Faraday::Connection, nil] inject a Faraday connection
    #   (useful for the test adapter or for sharing connection pools).
    # @param timeout [Integer] per-request timeout in seconds. Default 30.
    # @param user_agent [String, nil] override the default `User-Agent` header.
    def initialize(api_key:, base_url: nil, connection: nil, timeout: 30, user_agent: nil)
      if api_key.nil? || !api_key.is_a?(String) || api_key.strip.empty?
        raise Error.new(
          code: 'missing_api_key',
          message: 'A project API key is required to construct Rerout::Client.',
          status: 0
        )
      end

      @api_key = api_key
      @base_url = (base_url || DEFAULT_BASE_URL).to_s.sub(%r{/+\z}, '')
      @timeout = timeout
      @user_agent = user_agent || "rerout-ruby/#{Rerout::VERSION}"
      @connection = connection || default_connection

      @links = Resources::Links.new(self)
      @project = Resources::Project.new(self)
      @qr = Resources::Qr.new(self)
    end

    # Perform a JSON request against the Rerout API.
    #
    # @api private
    # @param method [Symbol] :get, :post, :patch, :delete
    # @param path [String] starts with `/`, includes any path params.
    # @param query [Hash, nil] query string params.
    # @param body [Object, nil] body to be JSON-encoded.
    # @return [Hash, Array, String, nil] parsed JSON body, raw text for non-JSON
    #   success bodies that the caller opted into via `raw: true`, or nil for
    #   204 No Content.
    def request(method:, path:, query: nil, body: nil, raw: false)
      headers = base_headers
      payload = nil
      if body
        payload = JSON.generate(body)
        headers['Content-Type'] = 'application/json'
      end

      response = perform_request(method: method, path: path, query: query,
                                 headers: headers, payload: payload)

      handle_response(response, raw: raw)
    end

    private

    def perform_request(method:, path:, query:, headers:, payload:)
      full_url = "#{@base_url}#{path}"
      @connection.public_send(method) do |req|
        req.url(full_url)
        req.params.update(query) if query && !query.empty?
        headers.each { |k, v| req.headers[k] = v }
        req.body = payload if payload
        req.options.timeout = @timeout if @timeout
        req.options.open_timeout = @timeout if @timeout
      end
    rescue Faraday::TimeoutError => e
      raise Error.new(code: 'timeout', message: e.message || 'Request timed out.', status: 0, details: e)
    rescue Faraday::ConnectionFailed, Faraday::SSLError => e
      raise Error.new(code: 'network_error', message: e.message || 'Network failure.', status: 0, details: e)
    rescue Faraday::Error => e
      raise Error.new(code: 'network_error', message: e.message || 'Faraday error.', status: 0, details: e)
    end

    def base_headers
      {
        'Authorization' => "Bearer #{@api_key}",
        'Accept' => 'application/json',
        'User-Agent' => @user_agent
      }
    end

    def handle_response(response, raw:)
      status = response.status
      body = response.body.to_s

      raise parse_error(status, body) unless status.between?(200, 299)
      return nil if status == 204 || body.empty?
      return body if raw

      begin
        JSON.parse(body)
      rescue JSON::ParserError => e
        raise Error.new(
          code: 'unexpected_response',
          message: 'Rerout returned a non-JSON success body.',
          status: status,
          details: { body: body, error: e.message }
        )
      end
    end

    def parse_error(status, body)
      if body.empty?
        return Error.new(
          code: synthetic_code(status),
          message: "Rerout returned HTTP #{status} with no body.",
          status: status
        )
      end

      begin
        parsed = JSON.parse(body)
      rescue JSON::ParserError
        return Error.new(
          code: synthetic_code(status),
          message: "Rerout returned HTTP #{status} (non-JSON body).",
          status: status,
          details: { body: body }
        )
      end

      Error.new(
        code: parsed['code'] || synthetic_code(status),
        message: parsed['message'] || "Rerout returned HTTP #{status}.",
        status: status,
        path: parsed['path'],
        timestamp: parsed['timestamp'],
        details: parsed
      )
    end

    def synthetic_code(status)
      case status
      when 401 then 'unauthorized'
      when 403 then 'forbidden'
      when 404 then 'not_found'
      when 429 then 'rate_limited'
      when 500..599 then 'server_error'
      else 'client_error'
      end
    end

    def default_connection
      Faraday.new(url: @base_url) do |conn|
        conn.request :url_encoded
        conn.adapter Faraday.default_adapter
      end
    end
  end
end
