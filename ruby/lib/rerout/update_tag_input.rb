# frozen_string_literal: true

module Rerout
  # Request body for `PATCH /v1/projects/me/tags/:tag_id`. Both fields are
  # optional; an omitted field is left unchanged. Unlike {UpdateLinkInput},
  # there is no client-side empty-payload check — mirroring the reference SDKs,
  # the server returns `400` for a fully empty patch. Neither field is nullable,
  # so there is no `Rerout::CLEAR` handling here.
  #
  # @example
  #   Rerout::UpdateTagInput.new(name: 'Renamed')
  #   Rerout::UpdateTagInput.new(color: 'red')
  class UpdateTagInput
    attr_reader :name, :color

    # @param name [String, nil] new label. Omitted when not set.
    # @param color [String, nil] new color. Omitted when not set.
    def initialize(name: OMIT, color: OMIT)
      @name = name
      @color = color
      freeze
    end

    # Serialize for the wire. Only fields that were set are included.
    def to_h
      hash = {}
      hash['name'] = name unless name.equal?(OMIT)
      hash['color'] = color unless color.equal?(OMIT)
      hash
    end

    # @return [Boolean] true when no field was set.
    def empty?
      to_h.empty?
    end
  end
end
