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
import java.util.Objects;

/**
 * A webhook endpoint registered to the project, as returned by the Rerout API.
 *
 * <p>Field names mirror the server-side {@code WebhookEndpointResponse} shape so
 * JSON is parsed without transformation. Instances are immutable.
 */
public final class Webhook {

    private final String id;

    @SerializedName("project_id")
    private final String projectId;

    private final String name;

    private final String url;

    private final List<String> events;

    @SerializedName("is_active")
    private final boolean active;

    @SerializedName("payload_format")
    private final String payloadFormat;

    @SerializedName("created_at")
    private final long createdAt;

    @SerializedName("updated_at")
    private final long updatedAt;

    @SerializedName("last_delivery_at")
    private final Long lastDeliveryAt;

    @SerializedName("last_success_at")
    private final Long lastSuccessAt;

    @SerializedName("last_failure_at")
    private final Long lastFailureAt;

    /**
     * Creates a {@code Webhook}. Normally constructed by the SDK's JSON decoder;
     * exposed for tests and manual construction.
     *
     * @param id             the endpoint identifier ({@code wh_…})
     * @param projectId      project that owns the endpoint
     * @param name           human-readable label for the endpoint
     * @param url            the public {@code https://} delivery URL
     * @param events         the event types this endpoint subscribes to; a
     *                       {@code null} value is normalised to an empty list
     * @param active         whether the endpoint is currently active
     * @param payloadFormat  the delivery payload encoding ({@code json} / {@code slack})
     * @param createdAt      endpoint creation time in unix seconds
     * @param updatedAt      last mutation time in unix seconds
     * @param lastDeliveryAt last delivery attempt in unix seconds, or {@code null}
     * @param lastSuccessAt  last successful delivery in unix seconds, or {@code null}
     * @param lastFailureAt  last failed delivery in unix seconds, or {@code null}
     */
    public Webhook(
            String id,
            String projectId,
            String name,
            String url,
            List<String> events,
            boolean active,
            String payloadFormat,
            long createdAt,
            long updatedAt,
            Long lastDeliveryAt,
            Long lastSuccessAt,
            Long lastFailureAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.url = url;
        this.events = events == null ? Collections.emptyList() : events;
        this.active = active;
        this.payloadFormat = payloadFormat;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastDeliveryAt = lastDeliveryAt;
        this.lastSuccessAt = lastSuccessAt;
        this.lastFailureAt = lastFailureAt;
    }

    /** {@return the endpoint identifier ({@code wh_…})} */
    public String getId() {
        return id;
    }

    /** {@return the identifier of the project that owns the endpoint} */
    public String getProjectId() {
        return projectId;
    }

    /** {@return the human-readable label for the endpoint} */
    public String getName() {
        return name;
    }

    /** {@return the public {@code https://} delivery URL} */
    public String getUrl() {
        return url;
    }

    /** {@return the event types this endpoint subscribes to; never {@code null}} */
    public List<String> getEvents() {
        return events == null ? Collections.emptyList() : events;
    }

    /** {@return whether the endpoint is currently active} */
    public boolean isActive() {
        return active;
    }

    /** {@return the delivery payload encoding ({@code json} / {@code slack})} */
    public String getPayloadFormat() {
        return payloadFormat;
    }

    /** {@return the endpoint creation time in unix seconds} */
    public long getCreatedAt() {
        return createdAt;
    }

    /** {@return the last mutation time in unix seconds} */
    public long getUpdatedAt() {
        return updatedAt;
    }

    /** {@return the last delivery attempt in unix seconds, or {@code null}} */
    public Long getLastDeliveryAt() {
        return lastDeliveryAt;
    }

    /** {@return the last successful delivery in unix seconds, or {@code null}} */
    public Long getLastSuccessAt() {
        return lastSuccessAt;
    }

    /** {@return the last failed delivery in unix seconds, or {@code null}} */
    public Long getLastFailureAt() {
        return lastFailureAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Webhook)) {
            return false;
        }
        Webhook other = (Webhook) o;
        return active == other.active
                && createdAt == other.createdAt
                && updatedAt == other.updatedAt
                && Objects.equals(id, other.id)
                && Objects.equals(projectId, other.projectId)
                && Objects.equals(name, other.name)
                && Objects.equals(url, other.url)
                && Objects.equals(getEvents(), other.getEvents())
                && Objects.equals(payloadFormat, other.payloadFormat)
                && Objects.equals(lastDeliveryAt, other.lastDeliveryAt)
                && Objects.equals(lastSuccessAt, other.lastSuccessAt)
                && Objects.equals(lastFailureAt, other.lastFailureAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, projectId, name, url, getEvents(), active, payloadFormat,
                createdAt, updatedAt, lastDeliveryAt, lastSuccessAt, lastFailureAt);
    }

    @Override
    public String toString() {
        return "Webhook{id=" + id + ", name=" + name + ", url=" + url
                + ", active=" + active + "}";
    }
}
