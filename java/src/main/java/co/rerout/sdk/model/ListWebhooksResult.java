/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

/** The webhook endpoints and supported event types returned by a list call. */
public final class ListWebhooksResult {

    private final List<Webhook> endpoints;

    @SerializedName("event_types")
    private final List<String> eventTypes;

    /**
     * Creates a {@code ListWebhooksResult}.
     *
     * @param endpoints  the webhook endpoints registered to the project; a
     *                   {@code null} value is normalised to an empty list
     * @param eventTypes every event type the server can deliver; a {@code null}
     *                   value is normalised to an empty list
     */
    public ListWebhooksResult(List<Webhook> endpoints, List<String> eventTypes) {
        this.endpoints = endpoints == null ? Collections.emptyList() : endpoints;
        this.eventTypes = eventTypes == null ? Collections.emptyList() : eventTypes;
    }

    /** {@return the webhook endpoints registered to the project; never {@code null}} */
    public List<Webhook> getEndpoints() {
        return endpoints == null ? Collections.emptyList() : endpoints;
    }

    /** {@return every event type the server can deliver; never {@code null}} */
    public List<String> getEventTypes() {
        return eventTypes == null ? Collections.emptyList() : eventTypes;
    }
}
