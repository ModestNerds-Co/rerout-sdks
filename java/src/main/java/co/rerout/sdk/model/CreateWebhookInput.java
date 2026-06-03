/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Request body for creating a webhook endpoint.
 *
 * <p>{@code name}, {@code url}, and at least one {@code event} are required.
 * Optional fields left {@code null} are omitted from the serialized JSON so
 * server-side defaults apply. Construct via {@link #builder(String, String)}.
 *
 * <pre>{@code
 * CreateWebhookInput input = CreateWebhookInput
 *     .builder("Order events", "https://example.com/hooks/rerout")
 *     .events("link.created", "link.clicked")
 *     .build();
 * }</pre>
 */
public final class CreateWebhookInput {

    private final String name;

    private final String url;

    private final List<String> events;

    @SerializedName("is_active")
    private final Boolean active;

    @SerializedName("payload_format")
    private final String payloadFormat;

    private CreateWebhookInput(Builder b) {
        this.name = b.name;
        this.url = b.url;
        this.events = Collections.unmodifiableList(new ArrayList<>(b.events));
        this.active = b.active;
        this.payloadFormat = b.payloadFormat;
    }

    /**
     * Starts a builder for a {@code CreateWebhookInput}.
     *
     * @param name the human-readable label for the endpoint (required)
     * @param url  the public {@code https://} URL that receives signed POST
     *             deliveries (required)
     * @return a new {@link Builder}
     */
    public static Builder builder(String name, String url) {
        return new Builder(name, url);
    }

    /** {@return the human-readable label for the endpoint} */
    public String getName() {
        return name;
    }

    /** {@return the public {@code https://} delivery URL} */
    public String getUrl() {
        return url;
    }

    /** {@return the event types to subscribe to; never {@code null}} */
    public List<String> getEvents() {
        return events;
    }

    /** {@return whether the endpoint starts active, or {@code null} for the default} */
    public Boolean getActive() {
        return active;
    }

    /** {@return the payload encoding, or {@code null} for the default} */
    public String getPayloadFormat() {
        return payloadFormat;
    }

    /** Fluent builder for {@link CreateWebhookInput}. */
    public static final class Builder {
        private final String name;
        private final String url;
        private final List<String> events = new ArrayList<>();
        private Boolean active;
        private String payloadFormat;

        private Builder(String name, String url) {
            this.name = name;
            this.url = url;
        }

        /**
         * Adds one or more event types to subscribe to (for example
         * {@code link.created}). At least one event is required. Calling this
         * multiple times accumulates events.
         *
         * @param events the event types to add
         * @return this builder
         */
        public Builder events(String... events) {
            this.events.addAll(Arrays.asList(events));
            return this;
        }

        /**
         * Adds the given event types to subscribe to. At least one event is
         * required. Calling this multiple times accumulates events.
         *
         * @param events the event types to add
         * @return this builder
         */
        public Builder events(List<String> events) {
            this.events.addAll(events);
            return this;
        }

        /**
         * Sets whether the endpoint starts active. Defaults to {@code true}
         * server-side when omitted.
         *
         * @param value {@code true} to start the endpoint active
         * @return this builder
         */
        public Builder isActive(boolean value) {
            this.active = value;
            return this;
        }

        /**
         * Sets the delivery payload encoding ({@code json} or {@code slack}).
         * Defaults to {@code json} server-side (or {@code slack} for Slack URLs)
         * when omitted.
         *
         * @param value the payload format
         * @return this builder
         */
        public Builder payloadFormat(String value) {
            this.payloadFormat = value;
            return this;
        }

        /**
         * Builds the immutable {@link CreateWebhookInput}.
         *
         * @return a new {@code CreateWebhookInput}
         */
        public CreateWebhookInput build() {
            return new CreateWebhookInput(this);
        }
    }
}
