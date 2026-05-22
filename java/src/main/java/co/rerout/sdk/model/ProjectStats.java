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

/** Aggregate analytics for a project across the requested window. */
public final class ProjectStats {

    private final int days;

    @SerializedName("total_clicks")
    private final long totalClicks;

    @SerializedName("qr_scans")
    private final long qrScans;

    private final List<DailyClicksPoint> daily;
    private final List<StatsBreakdown> countries;
    private final List<StatsBreakdown> referrers;
    private final List<StatsBreakdown> devices;
    private final List<StatsBreakdown> browsers;

    @SerializedName("top_codes")
    private final List<StatsBreakdown> topCodes;

    /**
     * Creates a {@code ProjectStats}.
     *
     * @param days        the window size in days the totals span
     * @param totalClicks total clicks recorded in the window
     * @param qrScans     total QR scans (a subset of {@code totalClicks})
     * @param daily       one point per day across the window
     * @param countries   top countries by click count
     * @param referrers   top referrers by click count
     * @param devices     click share by device class
     * @param browsers    click share by browser
     * @param topCodes    top short codes by click count
     */
    public ProjectStats(
            int days,
            long totalClicks,
            long qrScans,
            List<DailyClicksPoint> daily,
            List<StatsBreakdown> countries,
            List<StatsBreakdown> referrers,
            List<StatsBreakdown> devices,
            List<StatsBreakdown> browsers,
            List<StatsBreakdown> topCodes) {
        this.days = days;
        this.totalClicks = totalClicks;
        this.qrScans = qrScans;
        this.daily = daily;
        this.countries = countries;
        this.referrers = referrers;
        this.devices = devices;
        this.browsers = browsers;
        this.topCodes = topCodes;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    /** {@return the window size in days the totals span} */
    public int getDays() {
        return days;
    }

    /** {@return total clicks recorded in the window} */
    public long getTotalClicks() {
        return totalClicks;
    }

    /** {@return total QR scans (a subset of {@link #getTotalClicks()})} */
    public long getQrScans() {
        return qrScans;
    }

    /** {@return one point per day across the window; never {@code null}} */
    public List<DailyClicksPoint> getDaily() {
        return orEmpty(daily);
    }

    /** {@return top countries by click count; never {@code null}} */
    public List<StatsBreakdown> getCountries() {
        return orEmpty(countries);
    }

    /** {@return top referrers by click count; never {@code null}} */
    public List<StatsBreakdown> getReferrers() {
        return orEmpty(referrers);
    }

    /** {@return click share by device class; never {@code null}} */
    public List<StatsBreakdown> getDevices() {
        return orEmpty(devices);
    }

    /** {@return click share by browser; never {@code null}} */
    public List<StatsBreakdown> getBrowsers() {
        return orEmpty(browsers);
    }

    /** {@return top short codes by click count; never {@code null}} */
    public List<StatsBreakdown> getTopCodes() {
        return orEmpty(topCodes);
    }
}
