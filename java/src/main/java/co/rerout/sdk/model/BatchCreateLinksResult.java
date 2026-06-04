/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import java.util.Collections;
import java.util.List;

/**
 * Result of a batch link create ({@code POST /v1/links/batch}).
 *
 * <p>{@code created} is the number of links that succeeded out of {@code total}
 * submitted; {@code results} carries the per-link outcome in submission order.
 * Instances are immutable.
 */
public final class BatchCreateLinksResult {

    private final int created;

    private final int total;

    private final List<BatchLinkResult> results;

    /**
     * Creates a {@code BatchCreateLinksResult}.
     *
     * @param created the number of links created successfully
     * @param total   the number of links submitted
     * @param results the per-link outcomes in submission order; a {@code null}
     *                value is normalised to an empty list
     */
    public BatchCreateLinksResult(int created, int total, List<BatchLinkResult> results) {
        this.created = created;
        this.total = total;
        this.results = results == null ? Collections.emptyList() : results;
    }

    /** {@return the number of links created successfully} */
    public int getCreated() {
        return created;
    }

    /** {@return the number of links submitted} */
    public int getTotal() {
        return total;
    }

    /** {@return the per-link outcomes in submission order; never {@code null}} */
    public List<BatchLinkResult> getResults() {
        return results == null ? Collections.emptyList() : results;
    }
}
