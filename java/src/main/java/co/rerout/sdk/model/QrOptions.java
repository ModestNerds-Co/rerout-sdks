/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QR rendering parameters for {@code Qr.url} and {@code Qr.svg}.
 *
 * <p>All fields are optional — omit any to use the server default. Construct
 * via {@link #builder()}.
 *
 * <pre>{@code
 * QrOptions opts = QrOptions.builder()
 *     .size(12)
 *     .ecc("H")
 *     .domain("go.brand.com")
 *     .refreshEnabled()
 *     .build();
 * }</pre>
 */
public final class QrOptions {

    private final Integer size;
    private final Integer margin;
    private final String ecc;
    private final String domain;
    private final String refresh;

    private QrOptions(Builder b) {
        this.size = b.size;
        this.margin = b.margin;
        this.ecc = b.ecc;
        this.domain = b.domain;
        this.refresh = b.refresh;
    }

    /**
     * Starts a builder for a {@code QrOptions}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@return the module size in pixels, or {@code null} for the server default} */
    public Integer getSize() {
        return size;
    }

    /** {@return the quiet-zone modules, or {@code null} for the server default} */
    public Integer getMargin() {
        return margin;
    }

    /** {@return the error-correction level, or {@code null} for the server default} */
    public String getEcc() {
        return ecc;
    }

    /** {@return the custom domain to encode, or {@code null}} */
    public String getDomain() {
        return domain;
    }

    /**
     * {@return the resolved {@code refresh} query value, or {@code null} —
     * {@code "1"} when refresh was enabled, or the verbatim token}
     */
    public String getRefresh() {
        return refresh;
    }

    /**
     * Renders these options into ordered URL query pairs. Only the fields that
     * were set are included.
     *
     * @return an ordered, possibly empty map of query keys to values
     */
    public Map<String, String> toQueryParameters() {
        Map<String, String> out = new LinkedHashMap<>();
        if (size != null) {
            out.put("size", String.valueOf(size));
        }
        if (margin != null) {
            out.put("margin", String.valueOf(margin));
        }
        if (ecc != null) {
            out.put("ecc", ecc);
        }
        if (domain != null) {
            out.put("domain", domain);
        }
        if (refresh != null) {
            out.put("refresh", refresh);
        }
        return out;
    }

    /** Fluent builder for {@link QrOptions}. */
    public static final class Builder {
        private Integer size;
        private Integer margin;
        private String ecc;
        private String domain;
        private String refresh;

        private Builder() {
        }

        /**
         * Sets the module size in pixels (1–32; server default 8).
         *
         * @param value the module size
         * @return this builder
         */
        public Builder size(int value) {
            this.size = value;
            return this;
        }

        /**
         * Sets the quiet-zone margin in modules (0–16; server default 4).
         *
         * @param value the quiet-zone size
         * @return this builder
         */
        public Builder margin(int value) {
            this.margin = value;
            return this;
        }

        /**
         * Sets the error-correction level — {@code L}, {@code M}, {@code Q}, or
         * {@code H}.
         *
         * @param value the error-correction level
         * @return this builder
         */
        public Builder ecc(String value) {
            this.ecc = value;
            return this;
        }

        /**
         * Forces the QR to encode a specific verified custom domain.
         *
         * @param value the custom domain
         * @return this builder
         */
        public Builder domain(String value) {
            this.domain = value;
            return this;
        }

        /**
         * Enables cache-busting on regenerate. Emits {@code refresh=1}.
         *
         * @return this builder
         */
        public Builder refreshEnabled() {
            this.refresh = "1";
            return this;
        }

        /**
         * Sets a verbatim cache-bust token. Emits {@code refresh=<token>}.
         *
         * @param token the cache-bust token; ignored if {@code null} or empty
         * @return this builder
         */
        public Builder refreshToken(String token) {
            if (token != null && !token.isEmpty()) {
                this.refresh = token;
            }
            return this;
        }

        /**
         * Builds the immutable {@link QrOptions}.
         *
         * @return a new {@code QrOptions}
         */
        public QrOptions build() {
            return new QrOptions(this);
        }
    }
}
