<?php

declare(strict_types=1);

namespace Rerout\Models;

/**
 * Request body for `POST /v1/projects/me/webhooks`.
 *
 * `name`, `url`, and `events` are required. Unset (null) optional fields are
 * stripped from the outgoing payload — pass an explicit value or omit the
 * named argument entirely.
 */
final readonly class CreateWebhookInput
{
    /**
     * @param string       $name          Human-readable label for the endpoint.
     * @param string       $url           Public `https://` URL that receives signed POST deliveries.
     * @param list<string> $events        Event types to subscribe to (e.g. `link.created`). At least one.
     * @param bool|null    $isActive      Whether the endpoint starts active. Default: true (server-side).
     * @param string|null  $payloadFormat Payload encoding (`json` / `slack`). Default: `json` (server-side).
     */
    public function __construct(
        public string $name,
        public string $url,
        public array $events,
        public ?bool $isActive = null,
        public ?string $payloadFormat = null,
    ) {
    }

    /**
     * Render to the JSON-serialisable shape the API expects. Null optional
     * fields are stripped so an unset field doesn't override the server default.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        $out = [
            'name' => $this->name,
            'url' => $this->url,
            'events' => array_values($this->events),
        ];
        if ($this->isActive !== null) {
            $out['is_active'] = $this->isActive;
        }
        if ($this->payloadFormat !== null) {
            $out['payload_format'] = $this->payloadFormat;
        }

        return $out;
    }
}
