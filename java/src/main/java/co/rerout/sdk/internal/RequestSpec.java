/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.internal;

import java.util.Collections;
import java.util.Map;

/**
 * A fully-described request handed to {@link HttpTransport#executeAsync} and
 * {@link HttpTransport#executeBlocking}. Internal to the SDK.
 */
public final class RequestSpec {

    private final HttpTransport.Method method;
    private final String path;
    private final Map<String, String> query;
    private final String body;

    private RequestSpec(
            HttpTransport.Method method,
            String path,
            Map<String, String> query,
            String body) {
        this.method = method;
        this.path = path;
        this.query = query == null ? Collections.emptyMap() : query;
        this.body = body;
    }

    /**
     * Creates a request with no query parameters and no body.
     *
     * @param method the HTTP verb
     * @param path   the API path
     * @return a new {@code RequestSpec}
     */
    public static RequestSpec of(HttpTransport.Method method, String path) {
        return new RequestSpec(method, path, null, null);
    }

    /**
     * Creates a request with query parameters and no body.
     *
     * @param method the HTTP verb
     * @param path   the API path
     * @param query  the ordered query parameters
     * @return a new {@code RequestSpec}
     */
    public static RequestSpec of(
            HttpTransport.Method method,
            String path,
            Map<String, String> query) {
        return new RequestSpec(method, path, query, null);
    }

    /**
     * Creates a request with a JSON body and no query parameters.
     *
     * @param method the HTTP verb
     * @param path   the API path
     * @param body   the pre-serialized JSON body
     * @return a new {@code RequestSpec}
     */
    public static RequestSpec withBody(
            HttpTransport.Method method,
            String path,
            String body) {
        return new RequestSpec(method, path, null, body);
    }

    /** {@return the HTTP verb} */
    public HttpTransport.Method method() {
        return method;
    }

    /** {@return the API path} */
    public String path() {
        return path;
    }

    /** {@return the ordered query parameters; never {@code null}} */
    public Map<String, String> query() {
        return query;
    }

    /** {@return the pre-serialized JSON body, or {@code null} for no body} */
    public String body() {
        return body;
    }
}
