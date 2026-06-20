/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * A {@link Tag} together with the number of live (non-deleted) links it is
 * attached to. This is the list-only shape returned by
 * {@code Tags.list()} — {@code Tags.create} and {@code Tags.update} return a
 * plain {@link Tag} without a link count.
 *
 * <p>Field names mirror the server-side response so JSON is parsed without
 * transformation. Instances are immutable.
 */
public final class TagSummary {

    private final String id;
    private final String name;
    private final String color;

    @SerializedName("link_count")
    private final int linkCount;

    /**
     * Creates a {@code TagSummary}. Normally constructed by the SDK's JSON
     * decoder; exposed for tests and manual construction.
     *
     * @param id        the tag identifier
     * @param name      the tag display name
     * @param color     the tag colour
     * @param linkCount the number of live links the tag is attached to
     */
    public TagSummary(String id, String name, String color, int linkCount) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.linkCount = linkCount;
    }

    /** {@return the tag identifier} */
    public String getId() {
        return id;
    }

    /** {@return the tag display name} */
    public String getName() {
        return name;
    }

    /** {@return the tag colour} */
    public String getColor() {
        return color;
    }

    /** {@return the number of live (non-deleted) links the tag is attached to} */
    public int getLinkCount() {
        return linkCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TagSummary)) {
            return false;
        }
        TagSummary other = (TagSummary) o;
        return linkCount == other.linkCount
                && Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(color, other.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color, linkCount);
    }

    @Override
    public String toString() {
        return "TagSummary{id=" + id + ", name=" + name + ", color=" + color
                + ", linkCount=" + linkCount + "}";
    }
}
