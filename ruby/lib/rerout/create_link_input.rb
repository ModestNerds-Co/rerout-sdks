# frozen_string_literal: true

module Rerout
  # Request body for `POST /v1/links`. Only `target_url` is required —
  # everything else is optional and omitted from the payload when not set.
  class CreateLinkInput
    attr_reader :target_url, :domain_hostname, :code, :expires_at,
                :seo_title, :seo_description, :seo_image_url,
                :seo_canonical_url, :seo_noindex,
                :password, :max_clicks, :track_conversions,
                :routing_rules, :ab_variants

    # @param target_url [String] required, the destination URL.
    # @param domain_hostname [String, nil] verified custom domain (e.g. `go.brand.com`).
    # @param code [String, nil] custom path. Only allowed with a verified `domain_hostname`.
    # @param expires_at [Integer, nil] unix seconds.
    # @param seo_title [String, nil]
    # @param seo_description [String, nil]
    # @param seo_image_url [String, nil] absolute https:// URL.
    # @param seo_canonical_url [String, nil]
    # @param seo_noindex [Boolean, nil] default server-side: `true`.
    # @param password [String, nil] gate the link behind a password.
    # @param max_clicks [Integer, nil] disable the link after this many clicks.
    # @param track_conversions [Boolean, nil] enable conversion tracking.
    # @param routing_rules [Array<Rerout::Models::RoutingRule, Hash>, nil]
    #   Smart Link routing rules.
    # @param ab_variants [Array<Rerout::Models::AbVariant, Hash>, nil]
    #   A/B-test variants — each `{ target_url:, weight? }`.
    def initialize(target_url:, domain_hostname: nil, code: nil, expires_at: nil,
                   seo_title: nil, seo_description: nil, seo_image_url: nil,
                   seo_canonical_url: nil, seo_noindex: nil,
                   password: nil, max_clicks: nil, track_conversions: nil,
                   routing_rules: nil, ab_variants: nil)
      raise ArgumentError, 'target_url is required' if target_url.nil? || target_url.to_s.empty?

      @target_url = target_url
      @domain_hostname = domain_hostname
      @code = code
      @expires_at = expires_at
      @seo_title = seo_title
      @seo_description = seo_description
      @seo_image_url = seo_image_url
      @seo_canonical_url = seo_canonical_url
      @seo_noindex = seo_noindex
      @password = password
      @max_clicks = max_clicks
      @track_conversions = track_conversions
      @routing_rules = routing_rules
      @ab_variants = ab_variants
      freeze
    end

    # Serialize for the wire. Fields are only included when set.
    def to_h
      hash = { 'target_url' => target_url }
      hash['domain_hostname'] = domain_hostname unless domain_hostname.nil?
      hash['code'] = code unless code.nil?
      hash['expires_at'] = expires_at unless expires_at.nil?
      hash['seo_title'] = seo_title unless seo_title.nil?
      hash['seo_description'] = seo_description unless seo_description.nil?
      hash['seo_image_url'] = seo_image_url unless seo_image_url.nil?
      hash['seo_canonical_url'] = seo_canonical_url unless seo_canonical_url.nil?
      hash['seo_noindex'] = seo_noindex unless seo_noindex.nil?
      hash['password'] = password unless password.nil?
      hash['max_clicks'] = max_clicks unless max_clicks.nil?
      hash['track_conversions'] = track_conversions unless track_conversions.nil?
      hash['routing_rules'] = routing_rules.map { |r| InputSerialization.rule_hash(r) } unless routing_rules.nil?
      hash['ab_variants'] = ab_variants.map { |v| InputSerialization.variant_hash(v) } unless ab_variants.nil?
      hash
    end
  end
end
