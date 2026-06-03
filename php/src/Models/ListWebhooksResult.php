<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * List of webhook endpoints plus the full set of event types the server can
 * deliver. Mirrors the `GET /v1/projects/me/webhooks` response shape.
 */
final readonly class ListWebhooksResult
{
    /**
     * @param list<Webhook> $endpoints   Registered webhook endpoints.
     * @param list<string>  $eventTypes  Every event type the server can deliver.
     */
    public function __construct(
        public array $endpoints,
        public array $eventTypes,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $rawEndpoints = $data['endpoints'] ?? [];
        $endpoints = [];
        if (is_array($rawEndpoints)) {
            foreach ($rawEndpoints as $row) {
                if (is_array($row)) {
                    /** @var array<string, mixed> $row */
                    $endpoints[] = Webhook::fromArray($row);
                }
            }
        }

        $rawTypes = $data['event_types'] ?? [];
        $eventTypes = [];
        if (is_array($rawTypes)) {
            foreach ($rawTypes as $type) {
                if (is_string($type)) {
                    $eventTypes[] = $type;
                }
            }
        }

        return new self($endpoints, $eventTypes);
    }
}
