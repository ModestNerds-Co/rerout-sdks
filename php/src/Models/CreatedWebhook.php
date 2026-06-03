<?php

declare(strict_types=1);

namespace Rerout\Models;

use InvalidArgumentException;

/**
 * Result of creating a webhook endpoint.
 *
 * The `signing_secret` (`whsec_…`) is returned **once** — store it now to
 * verify deliveries; it is never shown again.
 */
final readonly class CreatedWebhook
{
    public function __construct(
        public Webhook $endpoint,
        public string $signingSecret,
    ) {
    }

    /**
     * @param array<string, mixed> $data
     */
    public static function fromArray(array $data): self
    {
        $endpoint = $data['endpoint'] ?? null;
        if (!is_array($endpoint)) {
            throw new InvalidArgumentException("Expected object field 'endpoint' in CreatedWebhook payload.");
        }

        $secret = $data['signing_secret'] ?? null;
        if (!is_string($secret)) {
            throw new InvalidArgumentException("Expected string field 'signing_secret' in CreatedWebhook payload.");
        }

        /** @var array<string, mixed> $endpoint */

        return new self(
            endpoint: Webhook::fromArray($endpoint),
            signingSecret: $secret,
        );
    }
}
