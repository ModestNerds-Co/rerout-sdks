/*
 * rerout-kotlin — Official Kotlin/JVM SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk

/**
 * Exception thrown for any failed Rerout API call — bad request, auth issue,
 * rate-limit, or network-level failure.
 *
 * The [code] field carries the stable string identifier returned by the Rerout
 * API (for example `bad_target_url`, `rate_limited`, `not_found`) so callers
 * can branch on it without parsing the human-readable [message].
 *
 * For network or non-JSON failures the [code] is one of the synthetic
 * client-side values: `network_error`, `timeout`, `unexpected_response`,
 * `unauthorized`, `forbidden`, `not_found`, `rate_limited`, `server_error`,
 * `client_error`, `missing_api_key`, `bad_request`.
 */
public class ReroutException(
    /** Stable error code, from the API or synthesized client-side. */
    public val code: String,
    /** Human-readable error message. */
    message: String,
    /** HTTP status code, or `0` when the request never reached the server. */
    public val status: Int,
    /** The API path that caused the error, when known. */
    public val path: String? = null,
    /** The timestamp of the error (ISO 8601), when supplied by the API. */
    public val timestamp: String? = null,
    /** The raw response body or underlying error, useful for debugging. */
    public val details: Any? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    /** `true` when the failure is HTTP 5xx — a server-side issue. */
    public val isServerError: Boolean get() = status in 500..599

    /** `true` when the failure is HTTP 429 — caller should back off and retry. */
    public val isRateLimited: Boolean get() = status == 429

    override fun toString(): String =
        "ReroutException(code=$code, status=$status, message=$message, path=$path, timestamp=$timestamp)"
}
