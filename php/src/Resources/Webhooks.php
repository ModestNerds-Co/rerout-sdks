<?php

declare(strict_types=1);

namespace Rerout\Resources;

use Rerout\Models\CreatedWebhook;
use Rerout\Models\CreateWebhookInput;
use Rerout\Models\ListWebhooksResult;
use Rerout\Rerout;

/**
 * Webhook endpoint management namespace. Reached via `Rerout::webhooks()`.
 *
 * Manages the project's webhook endpoints (create, list, delete). The project
 * is resolved from the API key — there is no project id in the path. To
 * *verify* inbound webhook deliveries, use {@see \Rerout\Webhooks\SignatureVerifier}.
 */
final class Webhooks
{
    public function __construct(private readonly Rerout $client)
    {
    }

    /**
     * Create a webhook endpoint for the project that owns the API key. The
     * returned `signingSecret` is shown once — persist it to verify deliveries.
     */
    public function create(CreateWebhookInput $input): CreatedWebhook
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'POST',
            path: '/v1/projects/me/webhooks',
            body: $input->toArray(),
        );

        return CreatedWebhook::fromArray($response);
    }

    /** List webhook endpoints and the event types the server can deliver. */
    public function list(): ListWebhooksResult
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'GET',
            path: '/v1/projects/me/webhooks',
        );

        return ListWebhooksResult::fromArray($response);
    }

    /**
     * Soft-delete an endpoint and abandon its pending deliveries. Idempotent.
     *
     * @return bool Whether the server confirmed deletion.
     */
    public function delete(string $endpointId): bool
    {
        $response = $this->client->request(
            method: 'DELETE',
            path: '/v1/projects/me/webhooks/' . rawurlencode($endpointId),
        );

        if (is_array($response) && array_key_exists('deleted', $response)) {
            $deleted = $response['deleted'];

            return is_bool($deleted) ? $deleted : true;
        }

        return true;
    }
}
