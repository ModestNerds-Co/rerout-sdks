<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * Result of a batch link create (`POST /v1/links/batch`).
 *
 * `created` is the number of links that succeeded out of `total` submitted;
 * `results` carries the per-link outcome in submission order.
 */
final readonly class BatchCreateLinksResult
{
    /**
     * @param int                   $created Number of links created successfully.
     * @param int                   $total   Number of links submitted.
     * @param list<BatchLinkResult> $results Per-link outcomes, in submission order.
     */
    public function __construct(
        public int $created,
        public int $total,
        public array $results,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $created = $data['created'] ?? null;
        if (!is_int($created)) {
            throw new InvalidArgumentException("Expected int field 'created' in BatchCreateLinksResult payload.");
        }

        $total = $data['total'] ?? null;
        if (!is_int($total)) {
            throw new InvalidArgumentException("Expected int field 'total' in BatchCreateLinksResult payload.");
        }

        $rawResults = $data['results'] ?? [];
        $results = [];
        if (is_array($rawResults)) {
            foreach ($rawResults as $row) {
                if (is_array($row)) {
                    /** @var array<string, mixed> $row */
                    $results[] = BatchLinkResult::fromArray($row);
                }
            }
        }

        return new self(created: $created, total: $total, results: $results);
    }
}
