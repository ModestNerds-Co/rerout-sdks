<?php

declare(strict_types=1);

namespace Rerout\Resources;

use Rerout\Models\ProjectStats;
use Rerout\Rerout;

/**
 * Project-level operations namespace. Reached via `Rerout::project()`.
 */
final class Project
{
    public function __construct(private readonly Rerout $client)
    {
    }

    /** Aggregate stats across every link in the project. */
    public function stats(int $days = 30): ProjectStats
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'GET',
            path: '/v1/projects/me/stats',
            query: ['days' => $days],
        );

        return ProjectStats::fromArray($response);
    }

    /**
     * Info about the project that owns the current API key.
     *
     * @return array<string, mixed>
     */
    public function me(): array
    {
        /** @var array<string, mixed> $response */
        $response = $this->client->request(
            method: 'GET',
            path: '/v1/projects/me',
        );

        return $response;
    }
}
