<?php

declare(strict_types=1);

namespace Rerout\Resources;

use Rerout\Models\QrOptions;
use Rerout\Rerout;

/**
 * QR helpers. Reached via `Rerout::qr()`.
 */
final class Qr
{
    public function __construct(private readonly Rerout $client)
    {
    }

    /**
     * Build the URL the Rerout API serves the QR SVG from. Pure — does not
     * call the API.
     */
    public function url(string $code, ?QrOptions $options = null): string
    {
        $url = $this->client->baseUrl() . '/v1/links/' . rawurlencode($code) . '/qr';

        if ($options === null) {
            return $url;
        }

        $params = $options->toQueryParameters();
        if ($params === []) {
            return $url;
        }

        return $url . '?' . http_build_query($params, '', '&', PHP_QUERY_RFC3986);
    }

    /**
     * Fetch the QR as an SVG string. Hits the same endpoint as `url()` but
     * attaches the bearer token and returns the rendered body.
     */
    public function svg(string $code, ?QrOptions $options = null): string
    {
        /** @var array<string, scalar|null> $query */
        $query = $options?->toQueryParameters() ?? [];

        $response = $this->client->request(
            method: 'GET',
            path: '/v1/links/' . rawurlencode($code) . '/qr',
            query: $query,
            expectJson: false,
        );

        return is_string($response) ? $response : '';
    }
}
