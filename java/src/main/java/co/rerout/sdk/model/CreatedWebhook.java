/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;

/**
 * Result of creating a webhook endpoint.
 *
 * <p>The {@code signingSecret} ({@code whsec_…}) is returned <strong>once</strong>
 * — store it now; it is never shown again. Use it with
 * {@link co.rerout.sdk.Webhooks#verifyReroutSignature} to verify inbound
 * deliveries. Instances are immutable.
 */
public final class CreatedWebhook {

    private final Webhook endpoint;

    @SerializedName("signing_secret")
    private final String signingSecret;

    /**
     * Creates a {@code CreatedWebhook}. Normally constructed by the SDK's JSON
     * decoder; exposed for tests and manual construction.
     *
     * @param endpoint      the newly created webhook endpoint
     * @param signingSecret the endpoint signing secret ({@code whsec_…}),
     *                      returned once
     */
    public CreatedWebhook(Webhook endpoint, String signingSecret) {
        this.endpoint = endpoint;
        this.signingSecret = signingSecret;
    }

    /** {@return the newly created webhook endpoint} */
    public Webhook getEndpoint() {
        return endpoint;
    }

    /**
     * {@return the endpoint signing secret ({@code whsec_…})}
     *
     * <p>Returned only on create — persist it immediately to verify deliveries.
     */
    public String getSigningSecret() {
        return signingSecret;
    }
}
