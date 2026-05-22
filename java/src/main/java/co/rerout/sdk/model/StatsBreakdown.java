/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

/** A single bucket in an analytics breakdown — e.g. one country, one device. */
public final class StatsBreakdown {

    private final String value;
    private final long clicks;

    /**
     * Creates a {@code StatsBreakdown}.
     *
     * @param value  the bucket label — country code, device class, browser name
     * @param clicks the click count for this bucket
     */
    public StatsBreakdown(String value, long clicks) {
        this.value = value;
        this.clicks = clicks;
    }

    /** {@return the bucket label} */
    public String getValue() {
        return value;
    }

    /** {@return the click count for this bucket} */
    public long getClicks() {
        return clicks;
    }
}
