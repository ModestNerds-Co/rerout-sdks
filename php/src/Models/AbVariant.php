<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * An A/B test variant on a Smart Link, as returned by the API. Incoming traffic
 * is split across the link's variants in proportion to each variant's `weight`.
 *
 * This is the response shape (it carries a server-assigned `id`). To *send*
 * variants on create/update use {@see AbVariantInput}.
 */
final readonly class AbVariant
{
    /**
     * @param int    $id        Server-assigned variant identifier.
     * @param string $targetUrl Destination this variant resolves to.
     * @param int    $weight    Relative traffic weight.
     */
    public function __construct(
        public int $id,
        public string $targetUrl,
        public int $weight,
    ) {
    }

    /**
     * Parse an {@see AbVariant} from an API JSON object.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $id = $data['id'] ?? null;
        if (!is_int($id)) {
            throw new InvalidArgumentException("Expected int field 'id' in AbVariant payload.");
        }

        $targetUrl = $data['target_url'] ?? null;
        if (!is_string($targetUrl)) {
            throw new InvalidArgumentException("Expected string field 'target_url' in AbVariant payload.");
        }

        $weight = $data['weight'] ?? null;
        if (!is_int($weight)) {
            throw new InvalidArgumentException("Expected int field 'weight' in AbVariant payload.");
        }

        return new self(id: $id, targetUrl: $targetUrl, weight: $weight);
    }
}
