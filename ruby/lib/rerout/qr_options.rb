# frozen_string_literal: true

module Rerout
  # Options for the QR endpoint. All fields are optional.
  #
  # - `size` (Integer): module size in px (1..32). Server default 8.
  # - `margin` (Integer): quiet zone in modules (0..16). Server default 4.
  # - `ecc` (String): error correction level. One of `L`, `M`, `Q`, `H`.
  # - `domain` (String): force the QR to encode a specific verified custom domain.
  # - `refresh` (String, true): cache-bust on regenerate. `true` is serialized as `"1"`.
  class QrOptions
    attr_reader :size, :margin, :ecc, :domain, :refresh

    ALLOWED_ECC = %w[L M Q H].freeze

    def initialize(size: nil, margin: nil, ecc: nil, domain: nil, refresh: nil)
      if ecc && !ALLOWED_ECC.include?(ecc.to_s)
        raise ArgumentError, "ecc must be one of #{ALLOWED_ECC.inspect}, got #{ecc.inspect}"
      end

      @size = size
      @margin = margin
      @ecc = ecc
      @domain = domain
      @refresh = refresh
      freeze
    end

    # @return [Boolean] true when no field is set.
    def empty?
      size.nil? && margin.nil? && ecc.nil? && domain.nil? && refresh.nil?
    end

    # Serialize options as a `{ key => string }` hash ready to be turned into
    # a query string. `refresh: true` becomes `"1"`.
    def to_query_hash
      out = {}
      out['size'] = size.to_s unless size.nil?
      out['margin'] = margin.to_s unless margin.nil?
      out['ecc'] = ecc.to_s unless ecc.nil?
      out['domain'] = domain.to_s unless domain.nil?
      unless refresh.nil?
        out['refresh'] = refresh == true ? '1' : refresh.to_s
      end
      out
    end
  end
end
