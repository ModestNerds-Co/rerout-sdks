<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * Paginated list of links.
 */
final readonly class ListLinksResult
{
    /**
     * @param list<Link> $links
     */
    public function __construct(
        public array $links,
        public ?int $nextCursor,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $raw = $data['links'] ?? [];
        $links = [];
        if (is_array($raw)) {
            foreach ($raw as $row) {
                if (is_array($row)) {
                    /** @var array<string, mixed> $row */
                    $links[] = Link::fromArray($row);
                }
            }
        }

        $cursor = $data['next_cursor'] ?? null;
        $nextCursor = is_int($cursor) ? $cursor : null;

        return new self($links, $nextCursor);
    }
}
