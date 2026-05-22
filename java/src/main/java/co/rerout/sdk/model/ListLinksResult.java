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

/** A paginated page of short links returned by {@code Links.list}. */
public final class ListLinksResult {

    private final List<Link> links;

    @SerializedName("next_cursor")
    private final Long nextCursor;

    /**
     * Creates a {@code ListLinksResult}.
     *
     * @param links      the links on this page, newest first
     * @param nextCursor the cursor for the next page, or {@code null} on the
     *                   last page
     */
    public ListLinksResult(List<Link> links, Long nextCursor) {
        this.links = links == null ? Collections.emptyList() : links;
        this.nextCursor = nextCursor;
    }

    /** {@return the links on this page, newest first; never {@code null}} */
    public List<Link> getLinks() {
        return links == null ? Collections.emptyList() : links;
    }

    /** {@return the cursor for the next page, or {@code null} on the last page} */
    public Long getNextCursor() {
        return nextCursor;
    }

    /** {@return whether more pages remain} */
    public boolean hasMore() {
        return nextCursor != null;
    }
}
