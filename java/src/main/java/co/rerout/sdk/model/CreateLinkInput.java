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
 * Request body for creating a short link.
 *
 * <p>Only {@code targetUrl} is required. Optional fields left {@code null} are
 * omitted from the serialized JSON so server-side defaults apply. Construct via
 * {@link #builder(String)}.
 *
 * <pre>{@code
 * CreateLinkInput input = CreateLinkInput.builder("https://example.com/sale")
 *     .domainHostname("go.brand.com")
 *     .code("q4")
 *     .seoTitle("Q4 Sale")
 *     .build();
 * }</pre>
 */
public final class CreateLinkInput {

    @SerializedName("target_url")
    private final String targetUrl;

    @SerializedName("domain_hostname")
    private final String domainHostname;

    private final String code;

    @SerializedName("expires_at")
    private final Long expiresAt;

    @SerializedName("seo_title")
    private final String seoTitle;

    @SerializedName("seo_description")
    private final String seoDescription;

    @SerializedName("seo_image_url")
    private final String seoImageUrl;

    @SerializedName("seo_canonical_url")
    private final String seoCanonicalUrl;

    @SerializedName("seo_noindex")
    private final Boolean seoNoindex;

    private final String password;

    @SerializedName("max_clicks")
    private final Integer maxClicks;

    @SerializedName("track_conversions")
    private final Boolean trackConversions;

    @SerializedName("routing_rules")
    private final List<RoutingRule> routingRules;

    @SerializedName("ab_variants")
    private final List<AbVariantInput> abVariants;

    private CreateLinkInput(Builder b) {
        this.targetUrl = b.targetUrl;
        this.domainHostname = b.domainHostname;
        this.code = b.code;
        this.expiresAt = b.expiresAt;
        this.seoTitle = b.seoTitle;
        this.seoDescription = b.seoDescription;
        this.seoImageUrl = b.seoImageUrl;
        this.seoCanonicalUrl = b.seoCanonicalUrl;
        this.seoNoindex = b.seoNoindex;
        this.password = b.password;
        this.maxClicks = b.maxClicks;
        this.trackConversions = b.trackConversions;
        this.routingRules = b.routingRules == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(b.routingRules));
        this.abVariants = b.abVariants == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(b.abVariants));
    }

    /**
     * Starts a builder for a {@code CreateLinkInput}.
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

    /** {@return the verified custom domain to host this link on, or {@code null}} */
    public String getDomainHostname() {
        return domainHostname;
    }

    /** {@return the custom path code, or {@code null}} */
    public String getCode() {
        return code;
    }

    /** {@return the expiry in unix seconds, or {@code null}} */
    public Long getExpiresAt() {
        return expiresAt;
    }

    /** {@return the social preview title override, or {@code null}} */
    public String getSeoTitle() {
        return seoTitle;
    }

    /** {@return the social preview description override, or {@code null}} */
    public String getSeoDescription() {
        return seoDescription;
    }

    /** {@return the social preview image URL override, or {@code null}} */
    public String getSeoImageUrl() {
        return seoImageUrl;
    }

    /** {@return the preview canonical URL override, or {@code null}} */
    public String getSeoCanonicalUrl() {
        return seoCanonicalUrl;
    }

    /** {@return whether the preview page is marked noindex, or {@code null}} */
    public Boolean getSeoNoindex() {
        return seoNoindex;
    }

    /** {@return the Smart Link password, or {@code null}} */
    public String getPassword() {
        return password;
    }

    /** {@return the click cap before the link stops resolving, or {@code null}} */
    public Integer getMaxClicks() {
        return maxClicks;
    }

    /** {@return whether conversion tracking is requested, or {@code null}} */
    public Boolean getTrackConversions() {
        return trackConversions;
    }

    /** {@return the smart-routing rules, or {@code null} if unset} */
    public List<RoutingRule> getRoutingRules() {
        return routingRules;
    }

    /** {@return the A/B test variants, or {@code null} if unset} */
    public List<AbVariantInput> getAbVariants() {
        return abVariants;
    }

    /** Fluent builder for {@link CreateLinkInput}. */
    public static final class Builder {
        private final String targetUrl;
        private String domainHostname;
        private String code;
        private Long expiresAt;
        private String seoTitle;
        private String seoDescription;
        private String seoImageUrl;
        private String seoCanonicalUrl;
        private Boolean seoNoindex;
        private String password;
        private Integer maxClicks;
        private Boolean trackConversions;
        private List<RoutingRule> routingRules;
        private List<AbVariantInput> abVariants;

        private Builder(String targetUrl) {
            this.targetUrl = targetUrl;
        }

        /**
         * Sets the verified custom domain to host this link on. Omit for a
         * {@code rerout.co/:code} link.
         *
         * @param value the domain hostname
         * @return this builder
         */
        public Builder domainHostname(String value) {
            this.domainHostname = value;
            return this;
        }

        /**
         * Sets the custom path code. Only valid when a {@code domainHostname}
         * is provided.
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
         * Sets the social preview title override. Maximum 90 characters.
         *
         * @param value the preview title
         * @return this builder
         */
        public Builder seoTitle(String value) {
            this.seoTitle = value;
            return this;
        }

        /**
         * Sets the social preview description override. Maximum 220 characters.
         *
         * @param value the preview description
         * @return this builder
         */
        public Builder seoDescription(String value) {
            this.seoDescription = value;
            return this;
        }

        /**
         * Sets the absolute {@code https://} social preview image URL.
         *
         * @param value the preview image URL
         * @return this builder
         */
        public Builder seoImageUrl(String value) {
            this.seoImageUrl = value;
            return this;
        }

        /**
         * Sets the canonical URL for the preview HTML.
         *
         * @param value the canonical URL
         * @return this builder
         */
        public Builder seoCanonicalUrl(String value) {
            this.seoCanonicalUrl = value;
            return this;
        }

        /**
         * Sets whether the preview page should be marked noindex.
         *
         * @param value {@code true} to mark the preview noindex
         * @return this builder
         */
        public Builder seoNoindex(boolean value) {
            this.seoNoindex = value;
            return this;
        }

        /**
         * Sets the Smart Link password. Visitors must enter it before being
         * redirected.
         *
         * @param value the password
         * @return this builder
         */
        public Builder password(String value) {
            this.password = value;
            return this;
        }

        /**
         * Sets the click cap before the link stops resolving.
         *
         * @param value the maximum clicks
         * @return this builder
         */
        public Builder maxClicks(int value) {
            this.maxClicks = value;
            return this;
        }

        /**
         * Enables or disables conversion tracking for this link.
         *
         * @param value {@code true} to track conversions
         * @return this builder
         */
        public Builder trackConversions(boolean value) {
            this.trackConversions = value;
            return this;
        }

        /**
         * Adds one or more smart-routing rules. Calling this multiple times
         * accumulates rules; rules are evaluated in the order added.
         *
         * @param rules the routing rules to add
         * @return this builder
         */
        public Builder routingRules(RoutingRule... rules) {
            if (this.routingRules == null) {
                this.routingRules = new ArrayList<>();
            }
            this.routingRules.addAll(Arrays.asList(rules));
            return this;
        }

        /**
         * Adds the given smart-routing rules. Calling this multiple times
         * accumulates rules; rules are evaluated in the order added.
         *
         * @param rules the routing rules to add
         * @return this builder
         */
        public Builder routingRules(List<RoutingRule> rules) {
            if (this.routingRules == null) {
                this.routingRules = new ArrayList<>();
            }
            this.routingRules.addAll(rules);
            return this;
        }

        /**
         * Adds one or more A/B test variants. Calling this multiple times
         * accumulates variants.
         *
         * @param variants the variants to add
         * @return this builder
         */
        public Builder abVariants(AbVariantInput... variants) {
            if (this.abVariants == null) {
                this.abVariants = new ArrayList<>();
            }
            this.abVariants.addAll(Arrays.asList(variants));
            return this;
        }

        /**
         * Adds the given A/B test variants. Calling this multiple times
         * accumulates variants.
         *
         * @param variants the variants to add
         * @return this builder
         */
        public Builder abVariants(List<AbVariantInput> variants) {
            if (this.abVariants == null) {
                this.abVariants = new ArrayList<>();
            }
            this.abVariants.addAll(variants);
            return this;
        }

        /**
         * Builds the immutable {@link CreateLinkInput}.
         *
         * @return a new {@code CreateLinkInput}
         */
        public CreateLinkInput build() {
            return new CreateLinkInput(this);
        }
    }
}
