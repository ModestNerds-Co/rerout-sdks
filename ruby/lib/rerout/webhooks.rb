# frozen_string_literal: true

require 'openssl'

module Rerout
  # Webhook signature verification.
  #
  # Rerout signs every webhook delivery with an `X-Rerout-Signature` header
  # shaped as `t={unix_seconds},v1={hex_hmac_sha256}`. The HMAC is computed
  # over `"{timestamp}.{raw_body}"` with the endpoint signing secret
  # (`whsec_…`) as the key.
  #
  # @example Rack / Rails controller
  #   raw = request.body.read
  #   ok = Rerout::Webhooks.verify_signature(
  #     raw_body: raw,
  #     signature_header: request.headers['X-Rerout-Signature'],
  #     secret: ENV.fetch('REROUT_WEBHOOK_SECRET')
  #   )
  #   head(:unauthorized) and return unless ok
  module Webhooks
    # Default tolerance window in seconds between the `t=` timestamp and the
    # current time. Five minutes — protects against captured-replay attacks.
    DEFAULT_TOLERANCE_SECONDS = 300

    # Verify a Rerout webhook signature.
    #
    # Returns `true` only when the header parses cleanly, the timestamp is
    # within `tolerance_seconds` of `now`, and the computed HMAC matches the
    # supplied `v1` in constant time. Returns `false` for every failure mode —
    # it never raises.
    #
    # @param raw_body [String] the exact, unmodified request body bytes.
    # @param signature_header [String] value of the `X-Rerout-Signature` header.
    # @param secret [String] the endpoint signing secret (`whsec_…`).
    # @param tolerance_seconds [Integer] staleness window. `0` disables the
    #   timestamp check entirely. Defaults to 300.
    # @param now [Proc, #call, nil] injectable clock returning the current
    #   unix time in seconds. Defaults to `Time.now.to_i`. Useful for tests.
    # @return [Boolean]
    def self.verify_signature(raw_body:, signature_header:, secret:,
                              tolerance_seconds: DEFAULT_TOLERANCE_SECONDS,
                              now: nil)
      return false if raw_body.nil?
      return false if signature_header.nil? || signature_header.to_s.empty?
      return false if secret.nil? || secret.to_s.empty?

      parsed = parse_header(signature_header.to_s)
      return false if parsed.nil?

      timestamp, v1 = parsed
      return false unless within_tolerance?(timestamp, tolerance_seconds, now)

      expected = OpenSSL::HMAC.hexdigest('SHA256', secret.to_s, "#{timestamp}.#{raw_body}")
      secure_compare(expected, v1)
    end

    # Parse `t=<unix>,v1=<hex>` (case-insensitive keys). Returns
    # `[timestamp, v1]` or `nil` when the header is unusable.
    #
    # @api private
    def self.parse_header(header)
      timestamp = nil
      v1 = nil
      header.split(',').each do |segment|
        eq = segment.index('=')
        next if eq.nil? || eq.zero?

        key = segment[0...eq].strip.downcase
        value = segment[(eq + 1)..].strip
        case key
        when 't'
          parsed = parse_timestamp(value)
          timestamp = parsed unless parsed.nil?
        when 'v1'
          v1 = value unless value.empty?
        end
      end
      return nil if timestamp.nil? || v1.nil?

      [timestamp, v1]
    end
    private_class_method :parse_header

    # Strict positive-integer parse — rejects `"abc"`, `"12x"`, `""`, `"-5"`.
    #
    # @api private
    def self.parse_timestamp(value)
      return nil unless value.match?(/\A\d+\z/)

      parsed = value.to_i
      parsed.positive? ? parsed : nil
    end
    private_class_method :parse_timestamp

    # @api private
    def self.within_tolerance?(timestamp, tolerance_seconds, now)
      return true if tolerance_seconds.to_i <= 0

      current = now ? now.call.to_i : Time.now.to_i
      (current - timestamp).abs <= tolerance_seconds.to_i
    end
    private_class_method :within_tolerance?

    # Constant-time string comparison over hex strings. Rejects non-hex or
    # odd-length `v1` values before comparing.
    #
    # @api private
    def self.secure_compare(expected, actual)
      return false if actual.nil? || actual.empty?
      return false unless actual.length.even?
      return false unless actual.match?(/\A[0-9a-fA-F]+\z/)
      return false unless expected.bytesize == actual.bytesize

      OpenSSL.fixed_length_secure_compare(
        expected.downcase, actual.downcase
      )
    rescue StandardError
      false
    end
    private_class_method :secure_compare
  end

  # Module-level convenience matching the BRIEF's free-function shape.
  #
  # @see Rerout::Webhooks.verify_signature
  # @return [Boolean]
  def self.verify_signature(raw_body:, signature_header:, secret:,
                            tolerance_seconds: Webhooks::DEFAULT_TOLERANCE_SECONDS,
                            now: nil)
    Webhooks.verify_signature(
      raw_body: raw_body,
      signature_header: signature_header,
      secret: secret,
      tolerance_seconds: tolerance_seconds,
      now: now
    )
  end
end
