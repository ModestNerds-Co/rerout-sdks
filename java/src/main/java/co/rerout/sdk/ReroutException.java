/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

/**
 * Unchecked exception thrown for any failed Rerout API call — a bad request, an
 * auth issue, a rate-limit, an invalid response shape, or a network-level
 * failure.
 *
 * <p>The {@link #getCode()} value carries the stable string identifier returned
 * by the Rerout API (for example {@code bad_target_url}, {@code rate_limited},
 * {@code not_found}) so callers can branch on it without parsing the
 * human-readable {@link #getMessage() message}.
 *
 * <p>For network or non-JSON failures the code is one of the synthetic
 * client-side values: {@code network_error}, {@code timeout},
 * {@code unexpected_response}, {@code unauthorized}, {@code forbidden},
 * {@code not_found}, {@code rate_limited}, {@code server_error},
 * {@code client_error}, {@code missing_api_key}, {@code bad_request}.
 *
 * <p>Both the blocking and async forms of every operation surface failures as
 * this type — the blocking form throws it directly, the async form completes
 * the returned {@link java.util.concurrent.CompletableFuture} exceptionally
 * with it.
 */
public final class ReroutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final int status;
    private final String path;
    private final String timestamp;
    private final transient Object details;

    /**
     * Creates a {@code ReroutException}.
     *
     * @param code      stable error code, from the API or synthesized client-side
     * @param message   human-readable error message
     * @param status    HTTP status code, or {@code 0} when the request never
     *                  reached the server
     * @param path      the API path that caused the error, or {@code null}
     * @param timestamp the ISO-8601 error timestamp supplied by the API, or
     *                  {@code null}
     * @param details   the raw response body or underlying error, useful for
     *                  debugging; may be {@code null}
     * @param cause     the underlying throwable, or {@code null}
     */
    public ReroutException(
            String code,
            String message,
            int status,
            String path,
            String timestamp,
            Object details,
            Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.path = path;
        this.timestamp = timestamp;
        this.details = details;
    }

    /**
     * Creates a {@code ReroutException} with no path, timestamp, details, or
     * cause.
     *
     * @param code    stable error code
     * @param message human-readable error message
     * @param status  HTTP status code, or {@code 0} for pre-flight failures
     */
    public ReroutException(String code, String message, int status) {
        this(code, message, status, null, null, null, null);
    }

    /**
     * Creates a {@code ReroutException} with a path but no timestamp, details,
     * or cause.
     *
     * @param code    stable error code
     * @param message human-readable error message
     * @param status  HTTP status code, or {@code 0} for pre-flight failures
     * @param path    the API path that caused the error
     */
    public ReroutException(String code, String message, int status, String path) {
        this(code, message, status, path, null, null, null);
    }

    /** {@return the stable error code, from the API or synthesized client-side} */
    public String getCode() {
        return code;
    }

    /**
     * {@return the HTTP status code, or {@code 0} when the request never
     * reached the server}
     */
    public int getStatus() {
        return status;
    }

    /** {@return the API path that caused the error, or {@code null} if unknown} */
    public String getPath() {
        return path;
    }

    /**
     * {@return the ISO-8601 error timestamp supplied by the API, or
     * {@code null} if absent}
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * {@return the raw response body or underlying error, or {@code null}}
     */
    public Object getDetails() {
        return details;
    }

    /** {@return {@code true} when the failure is HTTP 5xx — a server-side issue} */
    public boolean isServerError() {
        return status >= 500 && status < 600;
    }

    /**
     * {@return {@code true} when the failure is HTTP 429 — the caller should
     * back off and retry}
     */
    public boolean isRateLimited() {
        return status == 429;
    }

    @Override
    public String toString() {
        return "ReroutException(code=" + code
                + ", status=" + status
                + ", message=" + getMessage()
                + ", path=" + path
                + ", timestamp=" + timestamp + ")";
    }
}
