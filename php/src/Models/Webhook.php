<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * A webhook endpoint registered to the project, as returned by the Rerout API.
 *
 * Fields mirror the server-side `WebhookEndpointResponse` shape one-to-one so
 * JSON is parsed without transformation.
 */
final readonly class Webhook
{
    /**
     * @param string      $id             Endpoint id (`wh_…`).
     * @param string      $projectId      Project that owns the endpoint.
     * @param string      $name           Human-readable label.
     * @param string      $url            Public `https://` URL receiving signed POST deliveries.
     * @param list<string> $events        Event types the endpoint subscribes to.
     * @param bool        $isActive       Whether the endpoint is currently active.
     * @param string      $payloadFormat  Delivery payload encoding (`json` / `slack`).
     * @param int         $createdAt      Unix seconds — endpoint creation time.
     * @param int         $updatedAt      Unix seconds — last mutation.
     * @param int|null    $lastDeliveryAt Unix seconds — last delivery attempt. Null if never.
     * @param int|null    $lastSuccessAt  Unix seconds — last successful delivery. Null if never.
     * @param int|null    $lastFailureAt  Unix seconds — last failed delivery. Null if never.
     */
    public function __construct(
        public string $id,
        public string $projectId,
        public string $name,
        public string $url,
        public array $events,
        public bool $isActive,
        public string $payloadFormat,
        public int $createdAt,
        public int $updatedAt,
        public ?int $lastDeliveryAt,
        public ?int $lastSuccessAt,
        public ?int $lastFailureAt,
    ) {
    }

    /**
     * Parse a {@see Webhook} from an API JSON envelope.
     *
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        return new self(
            id: self::requireString($data, 'id'),
            projectId: self::requireString($data, 'project_id'),
            name: self::requireString($data, 'name'),
            url: self::requireString($data, 'url'),
            events: self::mapStrings($data['events'] ?? []),
            isActive: self::requireBool($data, 'is_active'),
            payloadFormat: self::requireString($data, 'payload_format'),
            createdAt: self::requireInt($data, 'created_at'),
            updatedAt: self::requireInt($data, 'updated_at'),
            lastDeliveryAt: self::nullableInt($data, 'last_delivery_at'),
            lastSuccessAt: self::nullableInt($data, 'last_success_at'),
            lastFailureAt: self::nullableInt($data, 'last_failure_at'),
        );
    }

    /**
     * @return list<string>
     */
    private static function mapStrings(mixed $raw): array
    {
        if (!is_array($raw)) {
            return [];
        }
        $out = [];
        foreach ($raw as $row) {
            if (is_string($row)) {
                $out[] = $row;
            }
        }

        return $out;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function requireString(array $data, string $key): string
    {
        $value = $data[$key] ?? null;
        if (!is_string($value)) {
            throw new InvalidArgumentException("Expected string field '{$key}' in Webhook payload.");
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
            throw new InvalidArgumentException("Expected int field '{$key}' in Webhook payload.");
        }

        return $value;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function nullableInt(array $data, string $key): ?int
    {
        $value = $data[$key] ?? null;
        if ($value === null) {
            return null;
        }
        if (!is_int($value)) {
            throw new InvalidArgumentException("Expected nullable int field '{$key}' in Webhook payload.");
        }

        return $value;
    }

    /**
     * @param array<string, mixed> $data
     */
    private static function requireBool(array $data, string $key): bool
    {
        $value = $data[$key] ?? null;
        if (!is_bool($value)) {
            throw new InvalidArgumentException("Expected bool field '{$key}' in Webhook payload.");
        }

        return $value;
    }
}
