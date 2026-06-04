/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request body for the {@code PATCH /v1/links/:code} endpoint.
 *
 * <p>Every field is optional. The shape distinguishes <em>"leave the field
 * alone"</em> from <em>"set the field to null on the server"</em> via explicit
 * {@code clear*()} builder methods: an unset field sends nothing, while a
 * {@code clear*()} call sends an explicit JSON {@code null}.
 *
 * <p>An empty patch (no setter and no {@code clear*()} call) is rejected
 * client-side by {@code Links.update} with code {@code bad_request} — it never
 * reaches the API.
 *
 * <pre>{@code
 * UpdateLinkInput patch = UpdateLinkInput.builder()
 *     .targetUrl("https://example.com/sale-extended")
 *     .clearExpiresAt()        // sends "expires_at": null
 *     .seoTitle("Extended Sale")
 *     .build();
 * }</pre>
 */
public final class UpdateLinkInput {

    private final String targetUrl;
    private final Long expiresAt;
    private final boolean clearExpiresAt;
    private final Boolean active;
    private final String seoTitle;
    private final boolean clearSeoTitle;
    private final String seoDescription;
    private final boolean clearSeoDescription;
    private final String seoImageUrl;
    private final boolean clearSeoImageUrl;
    private final String seoCanonicalUrl;
    private final boolean clearSeoCanonicalUrl;
    private final Boolean seoNoindex;
    private final String password;
    private final boolean clearPassword;
    private final Integer maxClicks;
    private final boolean clearMaxClicks;
    private final Boolean trackConversions;
    private final List<RoutingRule> routingRules;
    private final List<AbVariantInput> abVariants;

    private UpdateLinkInput(Builder b) {
        this.targetUrl = b.targetUrl;
        this.expiresAt = b.expiresAt;
        this.clearExpiresAt = b.clearExpiresAt;
        this.active = b.active;
        this.seoTitle = b.seoTitle;
        this.clearSeoTitle = b.clearSeoTitle;
        this.seoDescription = b.seoDescription;
        this.clearSeoDescription = b.clearSeoDescription;
        this.seoImageUrl = b.seoImageUrl;
        this.clearSeoImageUrl = b.clearSeoImageUrl;
        this.seoCanonicalUrl = b.seoCanonicalUrl;
        this.clearSeoCanonicalUrl = b.clearSeoCanonicalUrl;
        this.seoNoindex = b.seoNoindex;
        this.password = b.password;
        this.clearPassword = b.clearPassword;
        this.maxClicks = b.maxClicks;
        this.clearMaxClicks = b.clearMaxClicks;
        this.trackConversions = b.trackConversions;
        this.routingRules = b.routingRules == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(b.routingRules));
        this.abVariants = b.abVariants == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(b.abVariants));
    }

    /**
     * Starts a builder for an {@code UpdateLinkInput}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Renders this patch into the ordered map the API expects. A field is only
     * present when it was set, and a {@code clear*()} call maps to a literal
     * {@code null} value so an unset field never wipes server state.
     *
     * <p>The returned map's {@code null} values are intentional and meaningful.
     *
     * @return an ordered, possibly empty map of JSON keys to values
     */
    public Map<String, Object> toJsonMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (targetUrl != null) {
            map.put("target_url", targetUrl);
        }
        if (clearExpiresAt) {
            map.put("expires_at", null);
        } else if (expiresAt != null) {
            map.put("expires_at", expiresAt);
        }
        if (active != null) {
            map.put("is_active", active);
        }
        if (clearSeoTitle) {
            map.put("seo_title", null);
        } else if (seoTitle != null) {
            map.put("seo_title", seoTitle);
        }
        if (clearSeoDescription) {
            map.put("seo_description", null);
        } else if (seoDescription != null) {
            map.put("seo_description", seoDescription);
        }
        if (clearSeoImageUrl) {
            map.put("seo_image_url", null);
        } else if (seoImageUrl != null) {
            map.put("seo_image_url", seoImageUrl);
        }
        if (clearSeoCanonicalUrl) {
            map.put("seo_canonical_url", null);
        } else if (seoCanonicalUrl != null) {
            map.put("seo_canonical_url", seoCanonicalUrl);
        }
        if (seoNoindex != null) {
            map.put("seo_noindex", seoNoindex);
        }
        if (clearPassword) {
            map.put("password", null);
        } else if (password != null) {
            map.put("password", password);
        }
        if (clearMaxClicks) {
            map.put("max_clicks", null);
        } else if (maxClicks != null) {
            map.put("max_clicks", maxClicks);
        }
        if (trackConversions != null) {
            map.put("track_conversions", trackConversions);
        }
        if (routingRules != null) {
            map.put("routing_rules", routingRules);
        }
        if (abVariants != null) {
            map.put("ab_variants", abVariants);
        }
        return map;
    }

    /**
     * {@return {@code true} when no field is set — the API would reject this as
     * a no-op}
     */
    public boolean isEmpty() {
        return toJsonMap().isEmpty();
    }

    /** Fluent builder for {@link UpdateLinkInput}. */
    public static final class Builder {
        private String targetUrl;
        private Long expiresAt;
        private boolean clearExpiresAt;
        private Boolean active;
        private String seoTitle;
        private boolean clearSeoTitle;
        private String seoDescription;
        private boolean clearSeoDescription;
        private String seoImageUrl;
        private boolean clearSeoImageUrl;
        private String seoCanonicalUrl;
        private boolean clearSeoCanonicalUrl;
        private Boolean seoNoindex;
        private String password;
        private boolean clearPassword;
        private Integer maxClicks;
        private boolean clearMaxClicks;
        private Boolean trackConversions;
        private List<RoutingRule> routingRules;
        private List<AbVariantInput> abVariants;

        private Builder() {
        }

        /**
         * Sets a new destination URL.
         *
         * @param value the destination URL
         * @return this builder
         */
        public Builder targetUrl(String value) {
            this.targetUrl = value;
            return this;
        }

        /**
         * Sets a new expiry in unix seconds. Mutually exclusive in effect with
         * {@link #clearExpiresAt()} — the clear flag wins if both are called.
         *
         * @param value the expiry timestamp
         * @return this builder
         */
        public Builder expiresAt(long value) {
            this.expiresAt = value;
            return this;
        }

        /**
         * Sends {@code "expires_at": null} to remove an existing expiry.
         *
         * @return this builder
         */
        public Builder clearExpiresAt() {
            this.clearExpiresAt = true;
            return this;
        }

        /**
         * Activates or deactivates the link.
         *
         * @param value {@code true} to activate, {@code false} to deactivate
         * @return this builder
         */
        public Builder active(boolean value) {
            this.active = value;
            return this;
        }

        /**
         * Sets a new preview title.
         *
         * @param value the preview title
         * @return this builder
         */
        public Builder seoTitle(String value) {
            this.seoTitle = value;
            return this;
        }

        /**
         * Sends {@code "seo_title": null} to clear an existing title.
         *
         * @return this builder
         */
        public Builder clearSeoTitle() {
            this.clearSeoTitle = true;
            return this;
        }

        /**
         * Sets a new preview description.
         *
         * @param value the preview description
         * @return this builder
         */
        public Builder seoDescription(String value) {
            this.seoDescription = value;
            return this;
        }

        /**
         * Sends {@code "seo_description": null} to clear an existing description.
         *
         * @return this builder
         */
        public Builder clearSeoDescription() {
            this.clearSeoDescription = true;
            return this;
        }

        /**
         * Sets a new preview image URL.
         *
         * @param value the preview image URL
         * @return this builder
         */
        public Builder seoImageUrl(String value) {
            this.seoImageUrl = value;
            return this;
        }

        /**
         * Sends {@code "seo_image_url": null} to clear an existing image URL.
         *
         * @return this builder
         */
        public Builder clearSeoImageUrl() {
            this.clearSeoImageUrl = true;
            return this;
        }

        /**
         * Sets a new canonical URL.
         *
         * @param value the canonical URL
         * @return this builder
         */
        public Builder seoCanonicalUrl(String value) {
            this.seoCanonicalUrl = value;
            return this;
        }

        /**
         * Sends {@code "seo_canonical_url": null} to clear an existing canonical
         * URL.
         *
         * @return this builder
         */
        public Builder clearSeoCanonicalUrl() {
            this.clearSeoCanonicalUrl = true;
            return this;
        }

        /**
         * Toggles whether the preview page is noindex.
         *
         * @param value {@code true} to mark the preview noindex
         * @return this builder
         */
        public Builder seoNoindex(boolean value) {
            this.seoNoindex = value;
            return this;
        }

        /**
         * Sets a new Smart Link password. Mutually exclusive in effect with
         * {@link #clearPassword()} — the clear flag wins if both are called.
         *
         * @param value the password
         * @return this builder
         */
        public Builder password(String value) {
            this.password = value;
            return this;
        }

        /**
         * Sends {@code "password": null} to remove password protection.
         *
         * @return this builder
         */
        public Builder clearPassword() {
            this.clearPassword = true;
            return this;
        }

        /**
         * Sets a new click cap. Mutually exclusive in effect with
         * {@link #clearMaxClicks()} — the clear flag wins if both are called.
         *
         * @param value the maximum clicks
         * @return this builder
         */
        public Builder maxClicks(int value) {
            this.maxClicks = value;
            return this;
        }

        /**
         * Sends {@code "max_clicks": null} to remove an existing click cap.
         *
         * @return this builder
         */
        public Builder clearMaxClicks() {
            this.clearMaxClicks = true;
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
         * Replaces the link's smart-routing rules in full. Calling this
         * multiple times accumulates rules for the replacement; pass an empty
         * list to clear all rules.
         *
         * @param rules the routing rules to set
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
         * Replaces the link's smart-routing rules in full with the given rules.
         * Calling this multiple times accumulates rules for the replacement.
         *
         * @param rules the routing rules to set
         * @return this builder
         */
        public Builder routingRules(RoutingRule... rules) {
            if (this.routingRules == null) {
                this.routingRules = new ArrayList<>();
            }
            this.routingRules.addAll(java.util.Arrays.asList(rules));
            return this;
        }

        /**
         * Replaces the link's A/B test variants in full. Calling this multiple
         * times accumulates variants for the replacement; pass an empty list to
         * clear all variants.
         *
         * @param variants the variants to set
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
         * Replaces the link's A/B test variants in full with the given
         * variants. Calling this multiple times accumulates variants for the
         * replacement.
         *
         * @param variants the variants to set
         * @return this builder
         */
        public Builder abVariants(AbVariantInput... variants) {
            if (this.abVariants == null) {
                this.abVariants = new ArrayList<>();
            }
            this.abVariants.addAll(java.util.Arrays.asList(variants));
            return this;
        }

        /**
         * Builds the immutable {@link UpdateLinkInput}.
         *
         * @return a new {@code UpdateLinkInput}
         */
        public UpdateLinkInput build() {
            return new UpdateLinkInput(this);
        }
    }
}
