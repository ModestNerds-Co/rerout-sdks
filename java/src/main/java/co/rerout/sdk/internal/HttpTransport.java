/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.internal;

import co.rerout.sdk.ReroutException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Raw transport over the JDK's {@link HttpClient}. Handles auth, query params,
 * timeouts, error parsing, and the JSON / SVG content split.
 *
 * <p>The async path ({@link #executeAsync}) is primary; the blocking path
 * ({@link #executeBlocking}) joins it and unwraps {@link CompletionException}
 * so callers always see a clean {@link ReroutException}.
 *
 * <p>This type is internal — the public surface only exposes the idiomatic
 * namespace classes.
 */
public final class HttpTransport {

    /** Supported HTTP verbs for the Rerout API. */
    public enum Method {
        /** HTTP GET. */
        GET,
        /** HTTP POST. */
        POST,
        /** HTTP PATCH. */
        PATCH,
        /** HTTP DELETE. */
        DELETE
    }

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient client;
    private final Duration timeout;
    private final Gson gson;
    private final Gson nullSafeGson;
    private final String userAgent;

    /**
     * Creates a transport.
     *
     * @param apiKey    the project API key
     * @param baseUrl   the resolved base URL, trailing slashes already trimmed
     * @param client    the JDK HTTP client to send requests with
     * @param timeout   the per-request timeout
     * @param gson      the JSON codec
     * @param userAgent the {@code User-Agent} header value
     */
    public HttpTransport(
            String apiKey,
            String baseUrl,
            HttpClient client,
            Duration timeout,
            Gson gson,
            String userAgent) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.client = client;
        this.timeout = timeout;
        this.gson = gson;
        // A null-serializing variant for PATCH payloads, where an explicit
        // JSON `null` is a meaningful "clear this field" instruction. The
        // default `gson` keeps omitting nulls so `CreateLinkInput`'s optional
        // fields stay out of the wire body.
        this.nullSafeGson = gson.newBuilder().serializeNulls().create();
        this.userAgent = userAgent;
    }

    /**
     * {@return the JSON codec configured on this transport — omits {@code null}
     * fields}
     */
    public Gson gson() {
        return gson;
    }

    /**
     * {@return a JSON codec that serializes {@code null} values — used for
     * PATCH payloads where an explicit {@code null} clears a server field}
     */
    public Gson nullSafeGson() {
        return nullSafeGson;
    }

    /**
     * URL-encodes a value for use as a single path segment.
     *
     * <p>{@link URLEncoder} targets {@code application/x-www-form-urlencoded},
     * which encodes spaces as {@code +}. Path segments need {@code %20}, so the
     * {@code +} is fixed up afterwards.
     *
     * @param segment the raw path segment
     * @return the percent-encoded segment
     */
    public static String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Sends {@code request} asynchronously and completes with the response body
     * text on success, or exceptionally with a {@link ReroutException}.
     *
     * @param request the request to send
     * @return a future of the success response body
     */
    public CompletableFuture<String> executeAsync(RequestSpec request) {
        final HttpRequest httpRequest;
        try {
            httpRequest = buildRequest(request);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(
                    new ReroutException(
                            "bad_request",
                            "Failed to build the Rerout request: " + messageOf(e),
                            0,
                            request.path(),
                            null,
                            e,
                            e));
        }
        return client
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((response, throwable) -> {
                    if (throwable != null) {
                        throw new CompletionException(toReroutException(throwable, request.path()));
                    }
                    String body = response.body() == null ? "" : response.body();
                    int status = response.statusCode();
                    if (status < 200 || status >= 300) {
                        throw new CompletionException(parseError(status, body, request.path()));
                    }
                    return body;
                });
    }

    /**
     * Sends {@code request} and blocks until the response body text is
     * available.
     *
     * @param request the request to send
     * @return the success response body
     * @throws ReroutException on any failure
     */
    public String executeBlocking(RequestSpec request) {
        try {
            return executeAsync(request).join();
        } catch (CompletionException e) {
            throw unwrap(e, request.path());
        }
    }

    /**
     * Decodes a JSON success body into {@code type}.
     *
     * @param text the response body
     * @param type the target class
     * @param path the request path, for error context
     * @param <T>  the decoded type
     * @return the decoded value
     * @throws ReroutException with code {@code unexpected_response} when the
     *                         body is empty or not valid JSON for {@code type}
     */
    public <T> T decode(String text, Class<T> type, String path) {
        if (text == null || text.isEmpty()) {
            throw new ReroutException(
                    "unexpected_response",
                    "Rerout returned an empty success body.",
                    200,
                    path);
        }
        try {
            T value = gson.fromJson(text, type);
            if (value == null) {
                throw new ReroutException(
                        "unexpected_response",
                        "Rerout returned a JSON null where a value was expected.",
                        200,
                        path);
            }
            return value;
        } catch (JsonParseException e) {
            throw new ReroutException(
                    "unexpected_response",
                    "Rerout returned a body that could not be parsed as the expected type.",
                    200,
                    path,
                    null,
                    text,
                    e);
        }
    }

    /**
     * Unwraps a {@link CompletionException} thrown while joining an async call
     * into a {@link ReroutException}.
     *
     * @param e    the completion exception
     * @param path the request path, for synthetic-error context
     * @return the underlying {@code ReroutException}
     */
    public static ReroutException unwrap(CompletionException e, String path) {
        Throwable cause = e.getCause();
        if (cause instanceof ReroutException) {
            return (ReroutException) cause;
        }
        return new ReroutException(
                "network_error",
                "Request to Rerout failed: " + messageOf(cause != null ? cause : e),
                0,
                path,
                null,
                cause,
                cause);
    }

    // ─── internals ──────────────────────────────────────────────────────────

    private HttpRequest buildRequest(RequestSpec request) {
        StringBuilder url = new StringBuilder(baseUrl).append(request.path());
        Map<String, String> query = request.query();
        if (query != null && !query.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                url.append(first ? '?' : '&');
                first = false;
                url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json, image/svg+xml, text/plain")
                .header("User-Agent", userAgent);

        String body = request.body();
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        switch (request.method()) {
            case GET:
                builder.GET();
                break;
            case POST:
                builder.POST(body == null ? HttpRequest.BodyPublishers.ofString("") : publisher);
                break;
            case PATCH:
                builder.method(
                        "PATCH",
                        body == null ? HttpRequest.BodyPublishers.ofString("") : publisher);
                break;
            case DELETE:
                builder.method("DELETE", publisher);
                break;
            default:
                throw new IllegalStateException("Unsupported method: " + request.method());
        }
        return builder.build();
    }

    private static ReroutException toReroutException(Throwable throwable, String path) {
        Throwable cause = throwable;
        if (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause instanceof ReroutException) {
            return (ReroutException) cause;
        }
        boolean timedOut = cause instanceof HttpTimeoutException
                || cause instanceof HttpConnectTimeoutException
                || cause instanceof java.util.concurrent.TimeoutException
                || (cause instanceof IOException
                        && cause.getMessage() != null
                        && cause.getMessage().toLowerCase(java.util.Locale.ROOT).contains("timed out"));
        if (timedOut) {
            return new ReroutException(
                    "timeout",
                    "Request to Rerout timed out: " + messageOf(cause),
                    0,
                    path,
                    null,
                    cause,
                    cause);
        }
        return new ReroutException(
                "network_error",
                "Request to Rerout failed before the server replied: " + messageOf(cause),
                0,
                path,
                null,
                cause,
                cause);
    }

    private ReroutException parseError(int status, String body, String path) {
        if (body == null || body.isEmpty()) {
            return new ReroutException(
                    synthCodeForStatus(status),
                    "Rerout returned HTTP " + status + " with no body.",
                    status,
                    path);
        }
        try {
            JsonObject obj = gson.fromJson(body, JsonObject.class);
            if (obj == null) {
                return new ReroutException(
                        synthCodeForStatus(status),
                        "Rerout returned HTTP " + status + " (non-JSON body).",
                        status,
                        path,
                        null,
                        body,
                        null);
            }
            String code = stringMember(obj, "code");
            String message = stringMember(obj, "message");
            String timestamp = stringMember(obj, "timestamp");
            return new ReroutException(
                    code != null ? code : synthCodeForStatus(status),
                    message != null ? message : "Rerout returned HTTP " + status + ".",
                    status,
                    path,
                    timestamp,
                    body,
                    null);
        } catch (JsonSyntaxException e) {
            return new ReroutException(
                    synthCodeForStatus(status),
                    "Rerout returned HTTP " + status + " (non-JSON body).",
                    status,
                    path,
                    null,
                    body,
                    e);
        }
    }

    private static String stringMember(JsonObject obj, String name) {
        if (obj.has(name) && obj.get(name).isJsonPrimitive()) {
            return obj.get(name).getAsString();
        }
        return null;
    }

    /**
     * Maps a bare HTTP status (no JSON error body) to a synthetic error code.
     *
     * @param status the HTTP status code
     * @return the synthetic error code
     */
    public static String synthCodeForStatus(int status) {
        if (status == 401) {
            return "unauthorized";
        }
        if (status == 403) {
            return "forbidden";
        }
        if (status == 404) {
            return "not_found";
        }
        if (status == 429) {
            return "rate_limited";
        }
        if (status >= 500 && status < 600) {
            return "server_error";
        }
        return "client_error";
    }

    private static String messageOf(Throwable t) {
        if (t == null) {
            return "no detail";
        }
        String m = t.getMessage();
        return m != null ? m : t.getClass().getSimpleName();
    }
}
