# frozen_string_literal: true

module Rerout
  # Request body for `POST /v1/projects/me/tags`. `name` is required; `color`
  # is optional and omitted from the payload when not set (the server validates
  # it against its palette and defaults to `"teal"`).
  class CreateTagInput
    attr_reader :name, :color

    # @param name [String] required, the tag label.
    # @param color [String, nil] optional tag color. Server default: `"teal"`.
    def initialize(name:, color: nil)
      raise ArgumentError, 'name is required' if name.nil? || name.to_s.empty?

      @name = name
      @color = color
      freeze
    end

    # Serialize for the wire. `color` is only included when set.
    def to_h
      hash = { 'name' => name }
      hash['color'] = color unless color.nil?
      hash
    end
  end
end
