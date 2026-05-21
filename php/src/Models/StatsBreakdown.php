<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * One row of an aggregate stats breakdown — a dimension value paired with a
 * click count.
 */
final readonly class StatsBreakdown
{
    public function __construct(
        public string $value,
        public int $clicks,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $value = $data['value'] ?? null;
        $clicks = $data['clicks'] ?? null;
        if (!is_string($value)) {
            throw new InvalidArgumentException("Expected string field 'value' in StatsBreakdown payload.");
        }
        if (!is_int($clicks)) {
            throw new InvalidArgumentException("Expected int field 'clicks' in StatsBreakdown payload.");
        }

        return new self($value, $clicks);
    }
}
