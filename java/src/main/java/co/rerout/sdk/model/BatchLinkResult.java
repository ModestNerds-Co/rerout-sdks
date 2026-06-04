/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

/**
 * The outcome of a single link in a batch create. {@code index} ties the result
 * back to its position in the submitted {@code links} array. On success
 * {@code code} is set; on failure {@code error} carries the reason. Instances
 * are immutable.
 */
public final class BatchLinkResult {

    private final int index;

    private final boolean ok;

    private final String code;

    private final String error;

    /**
     * Creates a {@code BatchLinkResult}.
     *
     * @param index the position of this link in the submitted batch
     * @param ok    whether the link was created
     * @param code  the created link code, or {@code null} on failure
     * @param error the failure reason, or {@code null} on success
     */
    public BatchLinkResult(int index, boolean ok, String code, String error) {
        this.index = index;
        this.ok = ok;
        this.code = code;
        this.error = error;
    }

    /** {@return the position of this link in the submitted batch} */
    public int getIndex() {
        return index;
    }

    /** {@return whether the link was created} */
    public boolean isOk() {
        return ok;
    }

    /** {@return the created link code, or {@code null} on failure} */
    public String getCode() {
        return code;
    }

    /** {@return the failure reason, or {@code null} on success} */
    public String getError() {
        return error;
    }
}
