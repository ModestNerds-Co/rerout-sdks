# frozen_string_literal: true

require 'rerout'
require 'faraday'
require 'json'

RSpec.configure do |config|
  config.expect_with :rspec do |expectations|
    expectations.include_chain_clauses_in_custom_matcher_descriptions = true
  end

  config.mock_with :rspec do |mocks|
    mocks.verify_partial_doubles = true
  end

  config.shared_context_metadata_behavior = :apply_to_host_groups
  config.disable_monkey_patching!
  config.order = :random
  Kernel.srand config.seed
end

# Helpers for building a Rerout::Client wired to Faraday's in-memory test
# adapter. Each stub records the request so specs can assert on headers, query
# params and bodies without touching the network.
module ReroutTestHelpers
  # A single recorded request, captured by the test adapter stub. The verb is
  # stored as `http_method` to avoid shadowing `Struct#method`.
  RecordedRequest = Struct.new(:http_method, :url, :headers, :body, keyword_init: true)

  # Build a client whose Faraday connection routes through the test adapter.
  #
  # @param api_key [String]
  # @param base_url [String, nil]
  # @yield [Faraday::Adapter::Test::Stubs] register endpoint stubs.
  # @return [Array(Rerout::Client, Array<RecordedRequest>)]
  def build_client(api_key: 'rrk_test', base_url: nil, **client_opts)
    recorded = []
    stubs = Faraday::Adapter::Test::Stubs.new
    yield(stubs, recorded) if block_given?

    connection = Faraday.new do |conn|
      conn.adapter :test, stubs
    end

    client = Rerout::Client.new(
      api_key: api_key,
      base_url: base_url,
      connection: connection,
      **client_opts
    )
    [client, recorded]
  end

  # Register a stub that records the inbound request, then replies.
  #
  # @param stubs [Faraday::Adapter::Test::Stubs]
  # @param recorded [Array<RecordedRequest>]
  # @param method [Symbol] :get / :post / :patch / :delete
  # @param path [String] path to match (query string ignored by matcher).
  # @param reply [Hash] :status, :body, :headers for the canned response.
  def stub_endpoint(stubs, recorded, method:, path:, **reply)
    status = reply.fetch(:status, 200)
    body = reply.fetch(:body, '')
    headers = reply.fetch(:headers, { 'Content-Type' => 'application/json' })
    stubs.send(method, path) do |env|
      recorded << RecordedRequest.new(
        http_method: env.method,
        url: env.url,
        headers: env.request_headers,
        body: env.request_body
      )
      [status, headers, body]
    end
  end
end

RSpec.configure do |config|
  config.include ReroutTestHelpers
end
