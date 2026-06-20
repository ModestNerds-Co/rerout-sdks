<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * List of tags in the project plus their live link counts. Mirrors the
 * `GET /v1/projects/me/tags` response shape.
 */
final readonly class ListTagsResult
{
    /**
     * @param list<TagSummary> $tags Tags with their attached-link counts.
     */
    public function __construct(
        public array $tags,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $rawTags = $data['tags'] ?? [];
        $tags = [];
        if (is_array($rawTags)) {
            foreach ($rawTags as $row) {
                if (is_array($row)) {
                    /** @var array<string, mixed> $row */
                    $tags[] = TagSummary::fromArray($row);
                }
            }
        }

        return new self($tags);
    }
}
