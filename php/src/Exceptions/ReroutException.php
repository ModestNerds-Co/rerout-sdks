<?php

declare(strict_types=1);

namespace Rerout\Exceptions;

use RuntimeException;
use Throwable;

/**
 * Exception thrown for any failed Rerout API call — bad request, auth issue,
 * rate-limit, or network-level failure.
 *
 * The {@see ReroutException::$code} field carries the stable string identifier
 * returned by the Rerout API (for example `bad_target_url`, `rate_limited`,
 * `not_found`) so callers can branch on it without parsing the human-readable
 * {@see ReroutException::$message}.
 *
 * For network or non-JSON failures the code is one of the synthetic
 * client-side values: `network_error`, `timeout`, `unexpected_response`,
 * `unauthorized`, `forbidden`, `not_found`, `rate_limited`, `server_error`,
 * `client_error`, `missing_api_key`, `bad_request`.
 */
final class ReroutException extends RuntimeException
{
    /**
     * @param string                    $errorCode Stable error code, from the API or synthesized client-side.
     * @param string                    $message   Human-readable error message.
     * @param int                       $status    HTTP status code, or 0 when the request never reached the server.
     * @param string|null               $path      The API path that triggered the error, when known.
     * @param string|null               $timestamp Server-provided ISO-8601 timestamp, when supplied.
     * @param array<string, mixed>|null $details   Decoded JSON details body, when supplied.
     * @param Throwable|null            $previous  The underlying exception, if any.
     */
    public function __construct(
        public readonly string $errorCode,
        string $message,
        public readonly int $status,
        public readonly ?string $path = null,
        public readonly ?string $timestamp = null,
        public readonly ?array $details = null,
        ?Throwable $previous = null,
    ) {
        parent::__construct($message, 0, $previous);
    }

    /**
     * The stable Rerout error code. Aliased for ergonomic access — `$e->code`
     * mirrors the TypeScript and Dart SDKs.
     */
    public function code(): string
    {
        return $this->errorCode;
    }

    /**
     * HTTP status code (0 when the request never reached the server).
     */
    public function status(): int
    {
        return $this->status;
    }

    /**
     * True for HTTP 429 — caller should back off and retry.
     */
    public function isRateLimited(): bool
    {
        return $this->status === 429;
    }

    /**
     * True for HTTP 5xx responses (server-side issues).
     */
    public function isServerError(): bool
    {
        return $this->status >= 500 && $this->status < 600;
    }

    public function __toString(): string
    {
        return sprintf(
            'ReroutException(code: %s, status: %d, message: %s, path: %s)',
            $this->errorCode,
            $this->status,
            $this->getMessage(),
            $this->path ?? 'null',
        );
    }
}
