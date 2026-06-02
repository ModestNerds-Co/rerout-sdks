/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import java.util.Objects;

/**
 * A tag attached to a {@link Link}, as returned by the Rerout API.
 *
 * <p>Field names mirror the server-side {@code TagResponse} shape so JSON is
 * parsed without transformation. Tags are read-only for API-key clients.
 * Instances are immutable.
 */
public final class Tag {

    private final String id;
    private final String name;
    private final String color;

    /**
     * Creates a {@code Tag}. Normally constructed by the SDK's JSON decoder;
     * exposed for tests and manual construction.
     *
     * @param id    the tag identifier
     * @param name  the tag display name
     * @param color the tag colour, typically a hex string
     */
    public Tag(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    /** {@return the tag identifier} */
    public String getId() {
        return id;
    }

    /** {@return the tag display name} */
    public String getName() {
        return name;
    }

    /** {@return the tag colour, typically a hex string} */
    public String getColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag)) {
            return false;
        }
        Tag other = (Tag) o;
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(color, other.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color);
    }

    @Override
    public String toString() {
        return "Tag{id=" + id + ", name=" + name + ", color=" + color + "}";
    }
}
