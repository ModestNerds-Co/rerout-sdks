/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

/**
 * Result of recording a conversion ({@code POST /v1/conversions}).
 *
 * <p>{@code duplicate} is {@code true} when a conversion with the same click +
 * event was already recorded — the call is idempotent and the earlier record is
 * kept. Instances are immutable.
 */
public final class RecordedConversion {

    private final boolean recorded;

    private final boolean duplicate;

    /**
     * Creates a {@code RecordedConversion}.
     *
     * @param recorded  whether the conversion was accepted by the server
     * @param duplicate whether this conversion duplicated an existing one
     */
    public RecordedConversion(boolean recorded, boolean duplicate) {
        this.recorded = recorded;
        this.duplicate = duplicate;
    }

    /** {@return whether the conversion was accepted by the server} */
    public boolean isRecorded() {
        return recorded;
    }

    /** {@return whether this conversion duplicated an existing one} */
    public boolean isDuplicate() {
        return duplicate;
    }
}
