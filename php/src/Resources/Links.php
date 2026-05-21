<?php

declare(strict_types=1);

namespace Rerout\Resources;

use Rerout\Exceptions\ReroutException;
use Rerout\Models\CreateLinkInput;
use Rerout\Models\Link;
use Rerout\Models\LinkStats;
use Rerout\Models\ListLinksResult;
use Rerout\Models\UpdateLinkInput;
use Rerout\Rerout;

/**
 * Link operations namespace. Reached via `Rerout::links()`.
 */
final class Links
{
    public function __construct(private readonly Rerout $client)
    {
    }

    /** Create a new short link. */
    public function create(CreateLinkInput $input): Link
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'POST',
            path: '/v1/links',
            body: $input->toArray(),
        );

        return Link::fromArray($response);
    }

    /** Paginated list of links in the project. */
    public function list(?int $cursor = null, ?int $limit = null): ListLinksResult
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'GET',
            path: '/v1/links',
            query: ['cursor' => $cursor, 'limit' => $limit],
        );

        return ListLinksResult::fromArray($response);
    }

    /** Get a single link by code. */
    public function get(string $code): Link
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'GET',
            path: '/v1/links/' . rawurlencode($code),
        );

        return Link::fromArray($response);
    }

    /**
     * Patch a link. Only fields set on `$input` are sent. Throws
     * `ReroutException` with code `bad_request` when called with an empty
     * input — the API rejects no-op PATCH bodies.
     */
    public function update(string $code, UpdateLinkInput $input): Link
    {
        if ($input->isEmpty()) {
            throw new ReroutException(
                errorCode: 'bad_request',
                message: 'UpdateLinkInput has no fields to send.',
                status: 0,
                path: '/v1/links/' . $code,
            );
        }

        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'PATCH',
            path: '/v1/links/' . rawurlencode($code),
            body: $input->toArray(),
        );

        return Link::fromArray($response);
    }

    /**
     * Soft-delete a link. The short URL stops redirecting and is gone from
     * lists.
     *
     * @return bool Whether the server confirmed deletion.
     */
    public function delete(string $code): bool
    {
        $response = $this->client->request(
            method: 'DELETE',
            path: '/v1/links/' . rawurlencode($code),
        );

        if (is_array($response) && array_key_exists('deleted', $response)) {
            $deleted = $response['deleted'];

            return is_bool($deleted) ? $deleted : true;
        }

        return true;
    }

    /** Per-link click stats. Defaults to 30 days. */
    public function stats(string $code, int $days = 30): LinkStats
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'GET',
            path: '/v1/links/' . rawurlencode($code) . '/stats',
            query: ['days' => $days],
        );

        return LinkStats::fromArray($response);
    }
}
