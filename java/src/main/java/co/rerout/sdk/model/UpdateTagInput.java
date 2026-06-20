/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for the {@code PATCH /v1/projects/me/tags/:tag_id} endpoint.
 *
 * <p>Both {@code name} and {@code color} are optional. Only the fields set on
 * the builder are sent; an unset field is omitted entirely so the server leaves
 * it unchanged. Mirrors {@link UpdateLinkInput}'s "leave the field alone"
 * convention — neither {@code name} nor {@code color} is nullable on the
 * server, so there are no {@code clear*()} methods.
 *
 * <p>An empty patch (neither field set) is rejected client-side by
 * {@code Tags.update} with code {@code bad_request} — it never reaches the API,
 * matching {@code Links.update}.
 *
 * <pre>{@code
 * UpdateTagInput patch = UpdateTagInput.builder()
 *     .name("Renamed")
 *     .color("red")
 *     .build();
 * }</pre>
 */
public final class UpdateTagInput {

    private final String name;
    private final String color;

    private UpdateTagInput(Builder b) {
        this.name = b.name;
        this.color = b.color;
    }

    /**
     * Starts a builder for an {@code UpdateTagInput}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@return the new tag name, or {@code null} to leave it unchanged} */
    public String getName() {
        return name;
    }

    /** {@return the new tag colour, or {@code null} to leave it unchanged} */
    public String getColor() {
        return color;
    }

    /**
     * Renders this patch into the ordered map the API expects. A field is only
     * present when it was set; an unset field is omitted so server state is
     * left untouched.
     *
     * @return an ordered, possibly empty map of JSON keys to values
     */
    public Map<String, Object> toJsonMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (name != null) {
            map.put("name", name);
        }
        if (color != null) {
            map.put("color", color);
        }
        return map;
    }

    /**
     * {@return {@code true} when neither field is set — the API would reject
     * this as a no-op}
     */
    public boolean isEmpty() {
        return toJsonMap().isEmpty();
    }

    /** Fluent builder for {@link UpdateTagInput}. */
    public static final class Builder {
        private String name;
        private String color;

        private Builder() {
        }

        /**
         * Sets a new tag name.
         *
         * @param value the new name
         * @return this builder
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets a new tag colour.
         *
         * @param value the new colour
         * @return this builder
         */
        public Builder color(String value) {
            this.color = value;
            return this;
        }

        /**
         * Builds the immutable {@link UpdateTagInput}.
         *
         * @return a new {@code UpdateTagInput}
         */
        public UpdateTagInput build() {
            return new UpdateTagInput(this);
        }
    }
}
