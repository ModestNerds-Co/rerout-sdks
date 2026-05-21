# frozen_string_literal: true

module Rerout
  # Raised for any Rerout API failure — bad request, auth issue, rate limit,
  # network failure, timeout, or unparseable response.
  #
  # The {#code} field carries the stable string identifier returned by the
  # Rerout API (e.g. `bad_target_url`, `rate_limited`, `not_found`) so callers
  # can branch on it without parsing the human-readable {#message}.
  #
  # For network/timeout/parse failures the {#code} is one of the synthetic
  # values: `network_error`, `timeout`, `unexpected_response`, `unauthorized`,
  # `forbidden`, `not_found`, `rate_limited`, `server_error`, `client_error`,
  # `missing_api_key`.
  class Error < StandardError
    # @return [String] stable error code, either from the API or synthetic.
    attr_reader :code

    # @return [Integer] HTTP status, or 0 when the request never reached the server.
    attr_reader :status

    # @return [String, nil] API path that returned the error, when available.
    attr_reader :path

    # @return [String, nil] ISO-8601 server timestamp, when supplied.
    attr_reader :timestamp

    # @return [Object, nil] raw parsed payload or original cause.
    attr_reader :details

    def initialize(message:, code:, status: 0, path: nil, timestamp: nil, details: nil)
      super(message)
      @code = code
      @status = status
      @path = path
      @timestamp = timestamp
      @details = details
    end

    # @return [Boolean] true when the failure is HTTP 429.
    def rate_limited?
      status == 429
    end

    # @return [Boolean] true when the failure is HTTP 5xx.
    def server_error?
      status >= 500 && status < 600
    end

    def to_s
      "Rerout::Error(code: #{code}, status: #{status}, message: #{message}, " \
        "path: #{path.inspect}, timestamp: #{timestamp.inspect})"
    end
  end
end
