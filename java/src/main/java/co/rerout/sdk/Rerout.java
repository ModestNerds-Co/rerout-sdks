/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Branded link infrastructure on the edge. Create short links, render QR
 * codes, read analytics, and verify webhook signatures.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 *
 * @see <a href="https://rerout.co">https://rerout.co</a>
 * @see <a href="https://github.com/ModestNerds-Co/rerout-sdks">rerout-sdks</a>
 */

package co.rerout.sdk;

import co.rerout.sdk.internal.HttpTransport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Official client for the Rerout API.
 *
 * <p>The client exposes three namespaces — {@link #links()}, {@link #project()},
 * and {@link #qr()}. Every network operation ships in two forms:
 *
 * <ul>
 *   <li>a <strong>blocking</strong> method (for example {@code links().create(…)})
 *       that returns the value directly and throws {@link ReroutException} on
 *       failure;
 *   <li>an <strong>async</strong> method (for example
 *       {@code links().createAsync(…)}) that returns a
 *       {@link java.util.concurrent.CompletableFuture} which completes
 *       exceptionally with {@link ReroutException}.
 * </ul>
 *
 * <p>The async form is the primary path — it is implemented directly over
 * {@link HttpClient#sendAsync}. The blocking form simply joins it. Pick the
 * style that suits your call site; both reach the same transport.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * Rerout rerout = Rerout.create(System.getenv("REROUT_API_KEY"));
 *
 * // Blocking
 * Link link = rerout.links().create(
 *     CreateLinkInput.builder("https://example.com/q4-sale").code("q4").build());
 * System.out.println(link.getShortUrl());
 *
 * // Async
 * rerout.links().createAsync(
 *         CreateLinkInput.builder("https://example.com/q4-sale").build())
 *     .thenAccept(created -> System.out.println(created.getShortUrl()));
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe; reuse a single {@code Rerout}
 * across an application.
 */
public final class Rerout {

    /**
     * Default production API base URL. Override via {@link Builder#baseUrl}
     * for staging or a self-hosted setup.
     */
    public static final String DEFAULT_BASE_URL = "https://api.rerout.co";

    /** SDK version, surfaced in the {@code User-Agent} header. */
    public static final String SDK_VERSION = "0.1.0";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final String baseUrl;
    private final Links links;
    private final Project project;
    private final Qr qr;

    private Rerout(Builder b) {
        if (b.apiKey == null || b.apiKey.trim().isEmpty()) {
            throw new ReroutException(
                    "missing_api_key",
                    "A project API key is required to construct Rerout.",
                    0);
        }
        String resolved = (b.baseUrl == null ? DEFAULT_BASE_URL : b.baseUrl)
                .replaceAll("/+$", "");
        if (resolved.isEmpty()) {
            throw new ReroutException(
                    "bad_request",
                    "baseUrl must not be empty.",
                    0);
        }
        this.baseUrl = resolved;

        HttpClient httpClient = b.httpClient != null
                ? b.httpClient
                : HttpClient.newBuilder()
                        .connectTimeout(b.timeout != null ? b.timeout : DEFAULT_TIMEOUT)
                        .build();
        Gson gson = b.gson != null ? b.gson : new GsonBuilder().create();
        HttpTransport transport = new HttpTransport(
                b.apiKey,
                resolved,
                httpClient,
                b.timeout != null ? b.timeout : DEFAULT_TIMEOUT,
                gson,
                "rerout-java/" + SDK_VERSION);

        this.links = new Links(transport);
        this.project = new Project(transport);
        this.qr = new Qr(transport, resolved);
    }

    /**
     * Creates a client with the default base URL, a 30-second timeout, and a
     * JDK {@link HttpClient}.
     *
     * @param apiKey the project API key ({@code rrk_…}); a blank value throws
     *               {@link ReroutException} with code {@code missing_api_key}
     * @return a new client
     */
    public static Rerout create(String apiKey) {
        return new Builder(apiKey).build();
    }

    /**
     * Starts a {@link Builder} for advanced configuration — a custom base URL,
     * {@link HttpClient}, timeout, or {@link Gson} instance.
     *
     * @param apiKey the project API key ({@code rrk_…})
     * @return a new builder
     */
    public static Builder builder(String apiKey) {
        return new Builder(apiKey);
    }

    /** {@return the resolved API base URL — trailing slashes trimmed} */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** {@return the link operations namespace} */
    public Links links() {
        return links;
    }

    /** {@return the project operations namespace} */
    public Project project() {
        return project;
    }

    /** {@return the QR helpers namespace} */
    public Qr qr() {
        return qr;
    }

    /**
     * Fluent builder for {@link Rerout}.
     *
     * <p>Every field is optional except the API key, which is supplied to
     * {@link Rerout#builder(String)}. Defaults: {@link #DEFAULT_BASE_URL}, a
     * 30-second timeout, a fresh JDK {@link HttpClient}, and a default
     * {@link Gson}.
     */
    public static final class Builder {
        private final String apiKey;
        private String baseUrl;
        private HttpClient httpClient;
        private Duration timeout;
        private Gson gson;

        private Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * Overrides the API base URL. Trailing slashes are trimmed.
         *
         * @param value the base URL, for example a staging host
         * @return this builder
         */
        public Builder baseUrl(String value) {
            this.baseUrl = value;
            return this;
        }

        /**
         * Injects a pre-configured JDK {@link HttpClient} — useful for custom
         * executors, proxies, SSL contexts, or shared connection pools. When a
         * client is supplied, the per-request {@link #timeout} still applies
         * but the client's own connect timeout is whatever it was built with.
         *
         * @param value the HTTP client
         * @return this builder
         */
        public Builder httpClient(HttpClient value) {
            this.httpClient = value;
            return this;
        }

        /**
         * Sets the per-request timeout. Defaults to 30 seconds. Applies to the
         * full request including the response body.
         *
         * @param value the timeout
         * @return this builder
         */
        public Builder timeout(Duration value) {
            this.timeout = value;
            return this;
        }

        /**
         * Injects a custom {@link Gson} instance — useful to tune number or
         * date handling. When omitted, a default {@code Gson} is used.
         *
         * @param value the JSON codec
         * @return this builder
         */
        public Builder gson(Gson value) {
            this.gson = value;
            return this;
        }

        /**
         * Builds the immutable {@link Rerout} client.
         *
         * @return a new client
         * @throws ReroutException with code {@code missing_api_key} when the
         *                         API key is blank
         */
        public Rerout build() {
            return new Rerout(this);
        }
    }
}
