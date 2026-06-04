<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * Result of recording a conversion via `POST /v1/conversions`.
 *
 * `duplicate` is true when a conversion with the same click + event was already
 * recorded — the call is idempotent and the earlier record is kept.
 */
final readonly class RecordedConversion
{
    /**
     * @param bool $recorded  Whether the conversion was accepted by the server.
     * @param bool $duplicate Whether this conversion was a duplicate of an existing one.
     */
    public function __construct(
        public bool $recorded,
        public bool $duplicate,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $recorded = $data['recorded'] ?? null;
        if (!is_bool($recorded)) {
            throw new InvalidArgumentException("Expected bool field 'recorded' in RecordedConversion payload.");
        }

        $duplicate = $data['duplicate'] ?? null;
        if (!is_bool($duplicate)) {
            throw new InvalidArgumentException("Expected bool field 'duplicate' in RecordedConversion payload.");
        }

        return new self(recorded: $recorded, duplicate: $duplicate);
    }
}
