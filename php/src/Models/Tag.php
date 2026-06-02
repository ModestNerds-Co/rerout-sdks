<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * A tag attached to a link, as returned by the Rerout API.
 *
 * Fields mirror the server-side tag shape one-to-one so JSON is parsed without
 * transformation. Tags are read-only for API-key clients.
 */
final readonly class Tag
{
    /**
     * @param string $id    Tag identifier.
     * @param string $name  Human-readable tag name.
     * @param string $color Display colour (e.g. a hex string).
     */
    public function __construct(
        public string $id,
        public string $name,
        public string $color,
    ) {
    }

    /**
     * Parse a {@see Tag} from an API JSON object.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            id: self::requireString($data, 'id'),
            name: self::requireString($data, 'name'),
            color: self::requireString($data, 'color'),
        );
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function requireString(array $data, string $key): string
    {
        $value = $data[$key] ?? null;
        if (!is_string($value)) {
            throw new InvalidArgumentException("Expected string field '{$key}' in Tag payload.");
        }

        return $value;
    }
}
