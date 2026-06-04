<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * A Smart Link routing rule. When a rule matches the incoming request the link
 * resolves to the rule's `targetUrl` instead of the link's default destination.
 *
 * Used both in the {@see Link} response and as a request item on
 * {@see CreateLinkInput} / {@see UpdateLinkInput} (where it is rendered via
 * {@see RoutingRule::toArray()}).
 *
 * - `conditionType` is one of `country` / `device`.
 * - `conditionOp` is one of `is` / `is_not` / `in`.
 * - `conditionValue` is the value(s) to match against (e.g. `ZA`, `mobile`,
 *   or a comma-separated list for `in`).
 */
final readonly class RoutingRule
{
    /**
     * @param string $conditionType  Attribute to match — `country` or `device`.
     * @param string $conditionOp    Comparison operator — `is`, `is_not`, or `in`.
     * @param string $conditionValue Value(s) to match against.
     * @param string $targetUrl      Destination when the rule matches.
     */
    public function __construct(
        public string $conditionType,
        public string $conditionOp,
        public string $conditionValue,
        public string $targetUrl,
    ) {
    }

    /**
     * Parse a {@see RoutingRule} from an API JSON object.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            conditionType: self::requireString($data, 'condition_type'),
            conditionOp: self::requireString($data, 'condition_op'),
            conditionValue: self::requireString($data, 'condition_value'),
            targetUrl: self::requireString($data, 'target_url'),
        );
    }

    /**
     * Render to the JSON-serialisable shape the API expects.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        return [
            'condition_type' => $this->conditionType,
            'condition_op' => $this->conditionOp,
            'condition_value' => $this->conditionValue,
            'target_url' => $this->targetUrl,
        ];
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function requireString(array $data, string $key): string
    {
        $value = $data[$key] ?? null;
        if (!is_string($value)) {
            throw new InvalidArgumentException("Expected string field '{$key}' in RoutingRule payload.");
        }

        return $value;
    }
}
