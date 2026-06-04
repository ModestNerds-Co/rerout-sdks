/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;

/**
 * A single link item in a batch create ({@code POST /v1/links/batch}).
 *
 * <p>Only {@code targetUrl} is required. Optional fields left {@code null} are
 * omitted from the serialized JSON. Construct via {@link #builder(String)}.
 */
public final class BatchLinkInput {

    @SerializedName("target_url")
    private final String targetUrl;

    private final String code;

    @SerializedName("expires_at")
    private final Long expiresAt;

    @SerializedName("domain_hostname")
    private final String domainHostname;

    private BatchLinkInput(Builder b) {
        this.targetUrl = b.targetUrl;
        this.code = b.code;
        this.expiresAt = b.expiresAt;
        this.domainHostname = b.domainHostname;
    }

    /**
     * Starts a builder for a {@code BatchLinkInput}.
     *
     * @param targetUrl the absolute {@code https://} destination URL (required)
     * @return a new {@link Builder}
     */
    public static Builder builder(String targetUrl) {
        return new Builder(targetUrl);
    }

    /** {@return the absolute {@code https://} destination URL} */
    public String getTargetUrl() {
        return targetUrl;
    }

    /** {@return the custom path code, or {@code null}} */
    public String getCode() {
        return code;
    }

    /** {@return the expiry in unix seconds, or {@code null}} */
    public Long getExpiresAt() {
        return expiresAt;
    }

    /** {@return the verified custom domain to host this link on, or {@code null}} */
    public String getDomainHostname() {
        return domainHostname;
    }

    /** Fluent builder for {@link BatchLinkInput}. */
    public static final class Builder {
        private final String targetUrl;
        private String code;
        private Long expiresAt;
        private String domainHostname;

        private Builder(String targetUrl) {
            this.targetUrl = targetUrl;
        }

        /**
         * Sets the custom path code. Only valid with a verified domain.
         *
         * @param value the path code
         * @return this builder
         */
        public Builder code(String value) {
            this.code = value;
            return this;
        }

        /**
         * Sets the expiry in unix seconds. Omit for a permanent link.
         *
         * @param value the expiry timestamp
         * @return this builder
         */
        public Builder expiresAt(long value) {
            this.expiresAt = value;
            return this;
        }

        /**
         * Sets the verified custom domain to host this link on.
         *
         * @param value the domain hostname
         * @return this builder
         */
        public Builder domainHostname(String value) {
            this.domainHostname = value;
            return this;
        }

        /**
         * Builds the immutable {@link BatchLinkInput}.
         *
         * @return a new {@code BatchLinkInput}
         */
        public BatchLinkInput build() {
            return new BatchLinkInput(this);
        }
    }
}
