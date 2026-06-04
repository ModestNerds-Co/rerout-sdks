# frozen_string_literal: true

module Rerout
  # Internal helpers for serializing Smart Link sub-objects (routing rules and
  # A/B variants) accepted by {CreateLinkInput} / {UpdateLinkInput}. Each input
  # element may be a {Rerout::Models::RoutingRule} / {Rerout::Models::AbVariant}
  # value object or a plain Hash; both are coerced to the wire shape.
  #
  # @api private
  module InputSerialization
    module_function

    # @param rule [Rerout::Models::RoutingRule, Hash]
    # @return [Hash] string-keyed routing-rule payload.
    def rule_hash(rule)
      return rule.to_h if rule.is_a?(Models::RoutingRule)
      return stringify(rule) if rule.is_a?(Hash)

      raise ArgumentError, 'routing_rules entries must be Rerout::Models::RoutingRule or Hash'
    end

    # @param variant [Rerout::Models::AbVariant, Hash]
    # @return [Hash] string-keyed A/B-variant payload (no server `id`).
    def variant_hash(variant)
      return variant.to_h if variant.is_a?(Models::AbVariant)
      raise ArgumentError, 'ab_variants entries must be Rerout::Models::AbVariant or Hash' unless variant.is_a?(Hash)

      h = stringify(variant)
      h.delete('id')
      h['weight'] = 1 unless h.key?('weight')
      h
    end

    # Normalize a Hash to string keys without mutating the caller's object.
    def stringify(hash)
      hash.each_with_object({}) { |(k, v), out| out[k.to_s] = v }
    end
  end
end
