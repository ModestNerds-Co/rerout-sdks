/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

/** Result of a {@code Links.delete} call. */
public final class DeleteResult {

    private final boolean deleted;

    /**
     * Creates a {@code DeleteResult}.
     *
     * @param deleted whether the link was deleted
     */
    public DeleteResult(boolean deleted) {
        this.deleted = deleted;
    }

    /** {@return whether the link was deleted} */
    public boolean isDeleted() {
        return deleted;
    }
}
