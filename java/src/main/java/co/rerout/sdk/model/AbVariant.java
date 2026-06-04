/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * An A/B test variant on a Smart Link, as returned by the API. Incoming traffic
 * is split across the link's variants in proportion to each variant's
 * {@code weight}.
 *
 * <p>This is the response shape (it carries a server-assigned {@code id}). To
 * <em>send</em> variants on create/update use {@link AbVariantInput}.
 */
public final class AbVariant {

    private final int id;

    @SerializedName("target_url")
    private final String targetUrl;

    private final int weight;

    /**
     * Creates an {@code AbVariant}.
     *
     * @param id        the server-assigned variant identifier
     * @param targetUrl the destination this variant resolves to
     * @param weight    the relative traffic weight
     */
    public AbVariant(int id, String targetUrl, int weight) {
        this.id = id;
        this.targetUrl = targetUrl;
        this.weight = weight;
    }

    /** {@return the server-assigned variant identifier} */
    public int getId() {
        return id;
    }

    /** {@return the destination this variant resolves to} */
    public String getTargetUrl() {
        return targetUrl;
    }

    /** {@return the relative traffic weight} */
    public int getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbVariant)) {
            return false;
        }
        AbVariant other = (AbVariant) o;
        return id == other.id
                && weight == other.weight
                && Objects.equals(targetUrl, other.targetUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, targetUrl, weight);
    }

    @Override
    public String toString() {
        return "AbVariant{id=" + id + ", targetUrl=" + targetUrl + ", weight=" + weight + "}";
    }
}
