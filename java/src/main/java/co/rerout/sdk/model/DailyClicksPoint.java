/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;

/** A single point in a daily clicks time series. */
public final class DailyClicksPoint {

    private final long day;
    private final long clicks;

    @SerializedName("qr_scans")
    private final long qrScans;

    /**
     * Creates a {@code DailyClicksPoint}.
     *
     * @param day     the day bucket — unix seconds at 00:00 UTC
     * @param clicks  total clicks (link + QR) recorded that day
     * @param qrScans the subset of {@code clicks} that came from a QR scan
     */
    public DailyClicksPoint(long day, long clicks, long qrScans) {
        this.day = day;
        this.clicks = clicks;
        this.qrScans = qrScans;
    }

    /** {@return the day bucket — unix seconds at 00:00 UTC} */
    public long getDay() {
        return day;
    }

    /** {@return total clicks (link + QR) recorded that day} */
    public long getClicks() {
        return clicks;
    }

    /** {@return the subset of {@link #getClicks() clicks} that came from a QR scan} */
    public long getQrScans() {
        return qrScans;
    }
}
