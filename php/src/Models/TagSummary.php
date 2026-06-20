<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * A tag plus its live link count, as returned by `GET /v1/projects/me/tags`.
 *
 * This is the **list-only** shape — `create`/`update` responses return a plain
 * {@see Tag} without `linkCount`. Fields mirror the server-side shape so JSON is
 * parsed without transformation.
 */
final readonly class TagSummary
{
    /**
     * @param string $id        Tag identifier.
     * @param string $name      Human-readable tag name.
     * @param string $color     Display colour (e.g. `teal`).
     * @param int    $linkCount Number of live (non-deleted) links the tag is attached to.
     */
    public function __construct(
        public string $id,
        public string $name,
        public string $color,
        public int $linkCount,
    ) {
    }

    /**
     * Parse a {@see TagSummary} from an API JSON object.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            id: self::requireString($data, 'id'),
            name: self::requireString($data, 'name'),
            color: self::requireString($data, 'color'),
            linkCount: self::requireInt($data, 'link_count'),
        );
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function requireString(array $data, string $key): string
    {
        $value = $data[$key] ?? null;
        if (!is_string($value)) {
            throw new InvalidArgumentException("Expected string field '{$key}' in TagSummary payload.");
        }

        return $value;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function requireInt(array $data, string $key): int
    {
        $value = $data[$key] ?? null;
        if (!is_int($value)) {
            throw new InvalidArgumentException("Expected int field '{$key}' in TagSummary payload.");
        }

        return $value;
    }
}
