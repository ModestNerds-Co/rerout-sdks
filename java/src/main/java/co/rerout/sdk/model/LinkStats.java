/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

/** Analytics for a single short link across the requested window. */
public final class LinkStats {

    private final String code;
    private final int days;

    @SerializedName("total_clicks")
    private final long totalClicks;

    @SerializedName("qr_scans")
    private final long qrScans;

    private final List<StatsBreakdown> countries;
    private final List<StatsBreakdown> referrers;

    /**
     * Creates a {@code LinkStats}.
     *
     * @param code        the short code these stats belong to
     * @param days        the window size in days the totals span
     * @param totalClicks total clicks in the window
     * @param qrScans     the subset of {@code totalClicks} attributed to a QR scan
     * @param countries   top countries by click count
     * @param referrers   top referrers by click count
     */
    public LinkStats(
            String code,
            int days,
            long totalClicks,
            long qrScans,
            List<StatsBreakdown> countries,
            List<StatsBreakdown> referrers) {
        this.code = code;
        this.days = days;
        this.totalClicks = totalClicks;
        this.qrScans = qrScans;
        this.countries = countries;
        this.referrers = referrers;
    }

    /** {@return the short code these stats belong to} */
    public String getCode() {
        return code;
    }

    /** {@return the window size in days the totals span} */
    public int getDays() {
        return days;
    }

    /** {@return total clicks in the window} */
    public long getTotalClicks() {
        return totalClicks;
    }

    /** {@return the subset of {@link #getTotalClicks()} attributed to a QR scan} */
    public long getQrScans() {
        return qrScans;
    }

    /** {@return top countries by click count; never {@code null}} */
    public List<StatsBreakdown> getCountries() {
        return countries == null ? Collections.emptyList() : countries;
    }

    /** {@return top referrers by click count; never {@code null}} */
    public List<StatsBreakdown> getReferrers() {
        return referrers == null ? Collections.emptyList() : referrers;
    }
}
