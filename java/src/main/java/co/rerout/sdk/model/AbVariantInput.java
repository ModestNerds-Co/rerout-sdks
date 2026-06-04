/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;

/**
 * A request item for an A/B test variant on a Smart Link. Used in the
 * {@code abVariants} list of {@link CreateLinkInput} and
 * {@link UpdateLinkInput}.
 *
 * <p>Unlike {@link AbVariant} (the response shape) there is no {@code id} — the
 * server assigns one — and {@code weight} is optional; leave it {@code null} to
 * take the server default. The field names mirror the server shape so Gson
 * serializes it directly, omitting a {@code null} weight.
 */
public final class AbVariantInput {

    @SerializedName("target_url")
    private final String targetUrl;

    private final Integer weight;

    /**
     * Creates an {@code AbVariantInput} with the server-default weight.
     *
     * @param targetUrl the destination this variant resolves to
     */
    public AbVariantInput(String targetUrl) {
        this(targetUrl, null);
    }

    /**
     * Creates an {@code AbVariantInput}.
     *
     * @param targetUrl the destination this variant resolves to
     * @param weight    the relative traffic weight, or {@code null} for the
     *                  server default
     */
    public AbVariantInput(String targetUrl, Integer weight) {
        this.targetUrl = targetUrl;
        this.weight = weight;
    }

    /** {@return the destination this variant resolves to} */
    public String getTargetUrl() {
        return targetUrl;
    }

    /** {@return the relative traffic weight, or {@code null} for the default} */
    public Integer getWeight() {
        return weight;
    }
}
