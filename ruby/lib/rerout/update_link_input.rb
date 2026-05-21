# frozen_string_literal: true

module Rerout
  # Sentinel singleton used to distinguish "leave the field alone" (not passed,
  # default `Rerout::OMIT`) from "set the field to null on the server"
  # (`Rerout::CLEAR`).
  module ClearSentinel
    def self.inspect
      'Rerout::CLEAR'
    end
  end

  # @api private — internal default sentinel meaning "field not passed".
  module OmitSentinel
    def self.inspect
      'Rerout::OMIT'
    end
  end

  # Public sentinel — pass `Rerout::CLEAR` to a nullable field on
  # {Rerout::UpdateLinkInput} to send `null` on the wire.
  #
  # @example
  #   Rerout::UpdateLinkInput.new(seo_title: Rerout::CLEAR)
  CLEAR = ClearSentinel
  # Internal default — distinguishes "not provided" from "set to nil".
  OMIT = OmitSentinel

  # Request body for `PATCH /v1/links/:code`. Every field is optional. To
  # send `null` to the server (clear an existing value), pass `Rerout::CLEAR`
  # to that keyword argument.
  #
  # @example
  #   Rerout::UpdateLinkInput.new(target_url: 'https://new.example.com')
  #   Rerout::UpdateLinkInput.new(expires_at: Rerout::CLEAR)
  class UpdateLinkInput
    FIELDS = %i[
      target_url expires_at is_active seo_title seo_description
      seo_image_url seo_canonical_url seo_noindex
    ].freeze

    def initialize(target_url: OMIT, expires_at: OMIT, is_active: OMIT,
                   seo_title: OMIT, seo_description: OMIT, seo_image_url: OMIT,
                   seo_canonical_url: OMIT, seo_noindex: OMIT)
      @values = {
        target_url: target_url,
        expires_at: expires_at,
        is_active: is_active,
        seo_title: seo_title,
        seo_description: seo_description,
        seo_image_url: seo_image_url,
        seo_canonical_url: seo_canonical_url,
        seo_noindex: seo_noindex
      }
      freeze
    end

    # Serialize for the wire. Unset fields are omitted; `Rerout::CLEAR`
    # becomes `null`.
    def to_h
      out = {}
      FIELDS.each do |field|
        v = @values[field]
        next if v.equal?(OMIT)

        out[field.to_s] = v.equal?(CLEAR) ? nil : v
      end
      out
    end

    # @return [Boolean] true when no field was set.
    def empty?
      to_h.empty?
    end

    # @return [Object] the raw value (sentinel or actual) for a field.
    def value_for(field)
      @values.fetch(field)
    end
  end
end
