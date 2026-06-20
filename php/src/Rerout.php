<?php

declare(strict_types=1);

namespace Rerout;

use GuzzleHttp\Client;
use GuzzleHttp\ClientInterface;
use GuzzleHttp\Exception\ConnectException;
use GuzzleHttp\Exception\GuzzleException;
use GuzzleHttp\Exception\RequestException;
use GuzzleHttp\Exception\TransferException;
use GuzzleHttp\Psr7\Request as Psr7Request;
use JsonException;
use Psr\Http\Message\ResponseInterface;
use Rerout\Exceptions\ReroutException;
use Rerout\Resources\Conversions;
use Rerout\Resources\Links;
use Rerout\Resources\Project;
use Rerout\Resources\Qr;
use Rerout\Resources\Tags;
use Rerout\Resources\Webhooks;

/**
 * Official PHP client for the Rerout branded-link API.
 *
 * ## Usage
 *
 * ```php
 * use Rerout\Rerout;
 * use Rerout\Models\CreateLinkInput;
 *
 * $rerout = new Rerout(getenv('REROUT_API_KEY'));
 *
 * $link = $rerout->links()->create(new CreateLinkInput(
 *     targetUrl: 'https://example.com/q4-sale',
 *     domainHostname: 'go.brand.com',
 *     code: 'q4',
 * ));
 *
 * echo $link->shortUrl;
 * ```
 *
 * Construction options (passed as `$options` array):
 *
 * - `base_url` — override the API base URL. Defaults to `https://api.rerout.co`.
 * - `timeout` — request timeout in seconds. Defaults to 30.
 * - `client` — inject a pre-configured `\GuzzleHttp\ClientInterface` (e.g.
 *   for tests with a `MockHandler`).
 * - `default_headers` — extra headers appended to every request.
 */
final class Rerout
{
    /** Default production API URL. Override via the `base_url` option. */
    public const string DEFAULT_BASE_URL = 'https://api.rerout.co';

    /** Default per-request timeout in seconds. */
    public const int DEFAULT_TIMEOUT_SECONDS = 30;

    private readonly string $apiKey;
    private readonly string $baseUrl;
    private readonly int $timeout;
    private readonly ClientInterface $http;
    /** @var array<string, string> */
    private readonly array $defaultHeaders;

    private readonly Links $links;
    private readonly Project $project;
    private readonly Qr $qr;
    private readonly Webhooks $webhooks;
    private readonly Tags $tags;
    private readonly Conversions $conversions;

    /**
     * @param string                                                                              $apiKey  Project API key (`rrk_…`). Required.
     * @param array{base_url?: string, timeout?: int, client?: ClientInterface, default_headers?: array<string, string>} $options Construction options.
     */
    public function __construct(string $apiKey, array $options = [])
    {
        if (trim($apiKey) === '') {
            throw new ReroutException(
                errorCode: 'missing_api_key',
                message: 'A project API key is required to construct Rerout.',
                status: 0,
            );
        }
        $this->apiKey = $apiKey;

        $baseUrl = $options['base_url'] ?? self::DEFAULT_BASE_URL;
        $this->baseUrl = rtrim($baseUrl, '/');

        $this->timeout = $options['timeout'] ?? self::DEFAULT_TIMEOUT_SECONDS;

        $this->http = $options['client'] ?? new Client();

        $this->defaultHeaders = $options['default_headers'] ?? [];

        $this->links = new Links($this);
        $this->project = new Project($this);
        $this->qr = new Qr($this);
        $this->webhooks = new Webhooks($this);
        $this->tags = new Tags($this);
        $this->conversions = new Conversions($this);
    }

    /** Link operations: create, list, get, update, delete, stats. */
    public function links(): Links
    {
        return $this->links;
    }

    /** Project-level operations: aggregate stats, current project. */
    public function project(): Project
    {
        return $this->project;
    }

    /** QR helpers — URL builders and signed-fetch. */
    public function qr(): Qr
    {
        return $this->qr;
    }

    /** Webhook endpoint management: create, list, delete. */
    public function webhooks(): Webhooks
    {
        return $this->webhooks;
    }

    /** Tag management: list, create, update, delete. */
    public function tags(): Tags
    {
        return $this->tags;
    }

    /** Conversion tracking: record conversion events against a click. */
    public function conversions(): Conversions
    {
        return $this->conversions;
    }

    /** The resolved API base URL (trailing slashes stripped). */
    public function baseUrl(): string
    {
        return $this->baseUrl;
    }

    /**
     * @internal Used by {@see Links}, {@see Project}, {@see Qr}.
     *
     * Sends a request to the Rerout API and returns the decoded JSON body.
     * On 2xx with an empty body returns `null`. On error throws a
     * {@see ReroutException}. Set `$expectJson` to `false` to return the
     * raw response body as a string (used by the QR SVG fetch).
     *
     * @param array<string, scalar|null> $query
     * @param array<string, mixed>|null $body
     *
     * @return array<string, mixed>|string|null
     */
    public function request(
        string $method,
        string $path,
        array $query = [],
        ?array $body = null,
        bool $expectJson = true,
    ): array|string|null {
        $url = $this->baseUrl . $path;
        $filteredQuery = [];
        foreach ($query as $key => $value) {
            if ($value !== null) {
                $filteredQuery[$key] = (string) $value;
            }
        }
        if ($filteredQuery !== []) {
            $url .= '?' . http_build_query($filteredQuery, '', '&', PHP_QUERY_RFC3986);
        }

        $headers = [
            'Authorization' => 'Bearer ' . $this->apiKey,
            'Accept' => $expectJson ? 'application/json' : 'image/svg+xml,text/html',
        ];
        foreach ($this->defaultHeaders as $name => $value) {
            $headers[$name] = $value;
        }

        $bodyString = null;
        if ($body !== null) {
            try {
                $bodyString = json_encode($body, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
            } catch (JsonException $e) {
                throw new ReroutException(
                    errorCode: 'client_error',
                    message: 'Failed to encode request body as JSON: ' . $e->getMessage(),
                    status: 0,
                    path: $path,
                    previous: $e,
                );
            }
            $headers['Content-Type'] = 'application/json';
        }

        $request = new Psr7Request($method, $url, $headers, $bodyString);

        try {
            $response = $this->http->send($request, [
                'timeout' => $this->timeout,
                'connect_timeout' => $this->timeout,
                'http_errors' => false,
            ]);
        } catch (ConnectException $e) {
            throw new ReroutException(
                errorCode: self::isTimeout($e) ? 'timeout' : 'network_error',
                message: $e->getMessage(),
                status: 0,
                path: $path,
                previous: $e,
            );
        } catch (RequestException $e) {
            $resp = $e->getResponse();
            if ($resp !== null) {
                throw $this->errorFromResponse($resp, $path, $e);
            }

            throw new ReroutException(
                errorCode: self::isTimeout($e) ? 'timeout' : 'network_error',
                message: $e->getMessage(),
                status: 0,
                path: $path,
                previous: $e,
            );
        } catch (TransferException $e) {
            throw new ReroutException(
                errorCode: self::isTimeout($e) ? 'timeout' : 'network_error',
                message: $e->getMessage(),
                status: 0,
                path: $path,
                previous: $e,
            );
        } catch (GuzzleException $e) {
            throw new ReroutException(
                errorCode: 'network_error',
                message: $e->getMessage(),
                status: 0,
                path: $path,
                previous: $e,
            );
        }

        return $this->handleResponse($response, $path, $expectJson);
    }

    /**
     * @return array<string, mixed>|string|null
     */
    private function handleResponse(ResponseInterface $response, string $path, bool $expectJson): array|string|null
    {
        $status = $response->getStatusCode();
        $body = (string) $response->getBody();

        if ($status < 200 || $status >= 300) {
            throw $this->errorFromBody($status, $body, $path);
        }

        if (!$expectJson) {
            return $body;
        }

        if ($body === '') {
            return null;
        }

        try {
            /** @var array<string, mixed>|mixed $decoded */
            $decoded = json_decode($body, true, 512, JSON_THROW_ON_ERROR);
        } catch (JsonException $e) {
            throw new ReroutException(
                errorCode: 'unexpected_response',
                message: 'Rerout returned a non-JSON success body.',
                status: $status,
                path: $path,
                details: ['body' => $body],
                previous: $e,
            );
        }

        if (!is_array($decoded)) {
            throw new ReroutException(
                errorCode: 'unexpected_response',
                message: 'Rerout returned a non-object JSON success body.',
                status: $status,
                path: $path,
                details: ['body' => $body],
            );
        }

        /** @var array<string, mixed> $decoded */

        return $decoded;
    }

    private function errorFromResponse(ResponseInterface $response, string $path, ?\Throwable $previous = null): ReroutException
    {
        return $this->errorFromBody(
            $response->getStatusCode(),
            (string) $response->getBody(),
            $path,
            $previous,
        );
    }

    private function errorFromBody(int $status, string $body, string $path, ?\Throwable $previous = null): ReroutException
    {
        if ($body === '') {
            return new ReroutException(
                errorCode: self::synthCodeForStatus($status),
                message: sprintf('Rerout returned HTTP %d with no body.', $status),
                status: $status,
                path: $path,
                previous: $previous,
            );
        }

        $code = null;
        $message = null;
        $timestamp = null;
        $details = null;

        try {
            /** @var mixed $decoded */
            $decoded = json_decode($body, true, 512, JSON_THROW_ON_ERROR);
            if (is_array($decoded)) {
                /** @var array<string, mixed> $decoded */
                $details = $decoded;
                $rawCode = $decoded['code'] ?? null;
                if (is_string($rawCode)) {
                    $code = $rawCode;
                }
                $rawMessage = $decoded['message'] ?? null;
                if (is_string($rawMessage)) {
                    $message = $rawMessage;
                }
                $rawTimestamp = $decoded['timestamp'] ?? null;
                if (is_string($rawTimestamp)) {
                    $timestamp = $rawTimestamp;
                }
            }
        } catch (JsonException) {
            // Fall through and treat as a non-JSON error body.
        }

        return new ReroutException(
            errorCode: $code ?? self::synthCodeForStatus($status),
            message: $message ?? sprintf('Rerout returned HTTP %d.', $status),
            status: $status,
            path: $path,
            timestamp: $timestamp,
            details: $details,
            previous: $previous,
        );
    }

    private static function isTimeout(\Throwable $e): bool
    {
        $msg = strtolower($e->getMessage());

        return str_contains($msg, 'timed out') || str_contains($msg, 'timeout');
    }

    private static function synthCodeForStatus(int $status): string
    {
        return match (true) {
            $status === 401 => 'unauthorized',
            $status === 403 => 'forbidden',
            $status === 404 => 'not_found',
            $status === 429 => 'rate_limited',
            $status >= 500 && $status < 600 => 'server_error',
            default => 'client_error',
        };
    }
}
