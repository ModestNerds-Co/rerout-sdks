/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import java.util.Collections;
import java.util.List;

/** The tags returned by a {@code Tags.list()} call, each with its link count. */
public final class ListTagsResult {

    private final List<TagSummary> tags;

    /**
     * Creates a {@code ListTagsResult}.
     *
     * @param tags the project's tags; a {@code null} value is normalised to an
     *             empty list
     */
    public ListTagsResult(List<TagSummary> tags) {
        this.tags = tags == null ? Collections.emptyList() : tags;
    }

    /** {@return the project's tags with their live link counts; never {@code null}} */
    public List<TagSummary> getTags() {
        return tags == null ? Collections.emptyList() : tags;
    }
}
