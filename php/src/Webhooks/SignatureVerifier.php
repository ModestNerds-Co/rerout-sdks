<?php

declare(strict_types=1);

namespace Rerout\Webhooks;

/**
 * Verifier for inbound `X-Rerout-Signature` webhook headers.
 *
 * Rerout signs every webhook delivery as `t={unix_seconds},v1={hex_hmac_sha256}`
 * where the HMAC is computed over `"{timestamp}.{raw_body}"` with the endpoint
 * signing secret as the key.
 *
 * ## Usage
 *
 * ```php
 * use Rerout\Webhooks\SignatureVerifier;
 *
 * $ok = SignatureVerifier::verify(
 *     rawBody: file_get_contents('php://input'),
 *     signatureHeader: $_SERVER['HTTP_X_REROUT_SIGNATURE'] ?? '',
 *     secret: getenv('REROUT_WEBHOOK_SECRET'),
 * );
 *
 * if (!$ok) {
 *     http_response_code(401);
 *     exit;
 * }
 * ```
 */
final class SignatureVerifier
{
    /**
     * Default tolerance window in seconds between the `t=` timestamp and the
     * current time. Five minutes — protects against captured-replay attacks.
     */
    public const int DEFAULT_TOLERANCE_SECONDS = 300;

    /**
     * Returns true when `$signatureHeader` is a valid Rerout HMAC signature
     * for `$rawBody` under `$secret`.
     *
     * Returns false when the header is missing, malformed, the timestamp is
     * outside the tolerance window, or the HMAC doesn't match.
     *
     * @param string             $rawBody          The raw, unmodified request body.
     * @param string             $signatureHeader  Value of the `X-Rerout-Signature` header.
     * @param string             $secret           Endpoint signing secret (`whsec_…`).
     * @param int                $toleranceSeconds Staleness window. 0 disables the timestamp check.
     * @param (callable():int)|null $clock         Injectable clock for tests. Defaults to `time()`.
     */
    public static function verify(
        string $rawBody,
        string $signatureHeader,
        string $secret,
        int $toleranceSeconds = self::DEFAULT_TOLERANCE_SECONDS,
        ?callable $clock = null,
    ): bool {
        if ($signatureHeader === '' || $secret === '') {
            return false;
        }

        $parsed = self::parseHeader($signatureHeader);
        if ($parsed === null) {
            return false;
        }

        [$timestamp, $v1] = $parsed;

        if ($toleranceSeconds > 0) {
            $now = $clock !== null ? $clock() : time();
            if (abs($now - $timestamp) > $toleranceSeconds) {
                return false;
            }
        }

        $expectedHex = hash_hmac('sha256', $timestamp . '.' . $rawBody, $secret);

        $expectedBin = self::safeFromHex($expectedHex);
        $actualBin = self::safeFromHex($v1);

        if ($expectedBin === null || $actualBin === null) {
            return false;
        }
        if (strlen($expectedBin) !== strlen($actualBin)) {
            return false;
        }

        return hash_equals($expectedBin, $actualBin);
    }

    /**
     * @return array{0: int, 1: string}|null
     */
    private static function parseHeader(string $header): ?array
    {
        $timestamp = null;
        $v1 = null;

        foreach (explode(',', $header) as $segment) {
            $eq = strpos($segment, '=');
            if ($eq === false || $eq === 0) {
                continue;
            }
            $key = strtolower(trim(substr($segment, 0, $eq)));
            $value = trim(substr($segment, $eq + 1));

            if ($key === 't') {
                if ($value === '' || !preg_match('/^[0-9]+$/', $value)) {
                    continue;
                }
                $parsedTs = (int) $value;
                if ($parsedTs > 0) {
                    $timestamp = $parsedTs;
                }
            } elseif ($key === 'v1') {
                if ($value !== '') {
                    $v1 = $value;
                }
            }
        }

        if ($timestamp === null || $v1 === null) {
            return null;
        }

        return [$timestamp, $v1];
    }

    private static function safeFromHex(string $hex): ?string
    {
        if ($hex === '' || strlen($hex) % 2 !== 0) {
            return null;
        }
        if (!preg_match('/^[0-9a-fA-F]+$/', $hex)) {
            return null;
        }
        $bin = hex2bin($hex);

        return $bin === false ? null : $bin;
    }
}
