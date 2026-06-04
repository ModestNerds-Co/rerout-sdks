/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for recording a conversion ({@code POST /v1/conversions}).
 *
 * <p>{@code clickId} and {@code eventName} are required. Optional fields left
 * {@code null} are omitted from the serialized JSON. Construct via
 * {@link #builder(String, String)}.
 *
 * <pre>{@code
 * RecordConversionInput input = RecordConversionInput
 *     .builder("clk_123", "purchase")
 *     .valueCents(4999)
 *     .currency("USD")
 *     .build();
 * }</pre>
 */
public final class RecordConversionInput {

    @SerializedName("click_id")
    private final String clickId;

    @SerializedName("event_name")
    private final String eventName;

    @SerializedName("value_cents")
    private final Integer valueCents;

    private final String currency;

    private RecordConversionInput(Builder b) {
        this.clickId = b.clickId;
        this.eventName = b.eventName;
        this.valueCents = b.valueCents;
        this.currency = b.currency;
    }

    /**
     * Starts a builder for a {@code RecordConversionInput}.
     *
     * @param clickId   the identifier of the click that led to the conversion
     *                  (required)
     * @param eventName the name of the conversion event, for example
     *                  {@code purchase} (required)
     * @return a new {@link Builder}
     */
    public static Builder builder(String clickId, String eventName) {
        return new Builder(clickId, eventName);
    }

    /** {@return the identifier of the click that led to the conversion} */
    public String getClickId() {
        return clickId;
    }

    /** {@return the name of the conversion event} */
    public String getEventName() {
        return eventName;
    }

    /** {@return the monetary value in cents, or {@code null}} */
    public Integer getValueCents() {
        return valueCents;
    }

    /** {@return the ISO 4217 currency code, or {@code null}} */
    public String getCurrency() {
        return currency;
    }

    /** Fluent builder for {@link RecordConversionInput}. */
    public static final class Builder {
        private final String clickId;
        private final String eventName;
        private Integer valueCents;
        private String currency;

        private Builder(String clickId, String eventName) {
            this.clickId = clickId;
            this.eventName = eventName;
        }

        /**
         * Sets the monetary value of the conversion, in cents.
         *
         * @param value the value in cents
         * @return this builder
         */
        public Builder valueCents(int value) {
            this.valueCents = value;
            return this;
        }

        /**
         * Sets the ISO 4217 currency code for {@link #valueCents(int)}.
         *
         * @param value the currency code
         * @return this builder
         */
        public Builder currency(String value) {
            this.currency = value;
            return this;
        }

        /**
         * Builds the immutable {@link RecordConversionInput}.
         *
         * @return a new {@code RecordConversionInput}
         */
        public RecordConversionInput build() {
            return new RecordConversionInput(this);
        }
    }
}
