/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

import co.rerout.sdk.internal.HttpTransport;
import co.rerout.sdk.internal.HttpTransport.Method;
import co.rerout.sdk.internal.RequestSpec;
import co.rerout.sdk.model.QrOptions;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * QR helpers — a pure URL builder and an authenticated SVG fetch. Reached via
 * {@link Rerout#qr()}.
 *
 * <p>{@link #url} is a synchronous, no-network builder. {@link #svg} ships in
 * a blocking and an {@code …Async} form like the other namespaces.
 */
public final class Qr {

    private final HttpTransport transport;
    private final String resolvedBaseUrl;

    Qr(HttpTransport transport, String resolvedBaseUrl) {
        this.transport = transport;
        this.resolvedBaseUrl = resolvedBaseUrl;
    }

    // ─── url builder ────────────────────────────────────────────────────────

    /**
     * Builds the URL the Rerout API serves the QR SVG from. Pure — does not
     * call the API.
     *
     * @param code the short link code
     * @return the QR endpoint URL
     */
    public String url(String code) {
        return url(code, null);
    }

    /**
     * Builds the URL the Rerout API serves the QR SVG from, with rendering
     * options. Pure — does not call the API.
     *
     * <p>The QR endpoint is API-key authenticated, so the returned URL cannot
     * be embedded directly in a browser {@code <img>} tag — proxy it
     * server-side or use {@link #svg}.
     *
     * @param code    the short link code
     * @param options the rendering options, or {@code null} for all defaults
     * @return the QR endpoint URL
     */
    public String url(String code, QrOptions options) {
        StringBuilder url = new StringBuilder(resolvedBaseUrl)
                .append("/v1/links/")
                .append(HttpTransport.encodePathSegment(code))
                .append("/qr");
        Map<String, String> params = options == null
                ? Collections.emptyMap()
                : options.toQueryParameters();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(first ? '?' : '&');
            first = false;
            url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return url.toString();
    }

    // ─── svg fetch ──────────────────────────────────────────────────────────

    /**
     * Fetches the QR SVG body. Hits the same endpoint as {@link #url} but
     * attaches the bearer token and returns the rendered text.
     *
     * @param code the short link code
     * @return the SVG document text
     * @throws ReroutException on any failure
     */
    public String svg(String code) {
        return svg(code, null);
    }

    /**
     * Fetches the QR SVG body with rendering options.
     *
     * @param code    the short link code
     * @param options the rendering options, or {@code null} for all defaults
     * @return the SVG document text
     * @throws ReroutException on any failure
     */
    public String svg(String code, QrOptions options) {
        return join(svgAsync(code, options));
    }

    /**
     * Fetches the QR SVG body asynchronously.
     *
     * @param code the short link code
     * @return a future of the SVG document text
     */
    public CompletableFuture<String> svgAsync(String code) {
        return svgAsync(code, null);
    }

    /**
     * Fetches the QR SVG body with rendering options asynchronously.
     *
     * @param code    the short link code
     * @param options the rendering options, or {@code null} for all defaults
     * @return a future of the SVG document text
     */
    public CompletableFuture<String> svgAsync(String code, QrOptions options) {
        String path = "/v1/links/" + HttpTransport.encodePathSegment(code) + "/qr";
        Map<String, String> query = options == null
                ? Collections.emptyMap()
                : options.toQueryParameters();
        return transport
                .executeAsync(RequestSpec.of(Method.GET, path, query))
                .thenApply(body -> {
                    if (body == null || body.isEmpty()) {
                        throw new CompletionException(
                                new ReroutException(
                                        "unexpected_response",
                                        "Rerout returned an empty QR body.",
                                        200,
                                        path));
                    }
                    return body;
                });
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw HttpTransport.unwrap(e, null);
        }
    }
}
