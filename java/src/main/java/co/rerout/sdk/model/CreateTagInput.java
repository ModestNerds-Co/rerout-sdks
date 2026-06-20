/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

/**
 * Request body for creating a tag ({@code POST /v1/projects/me/tags}).
 *
 * <p>{@code name} is required. {@code color} is optional — left {@code null} it
 * is omitted from the serialized JSON so the server applies its default
 * ({@code teal}). Construct via {@link #builder(String)}.
 *
 * <pre>{@code
 * CreateTagInput input = CreateTagInput.builder("Spring 2026")
 *     .color("teal")
 *     .build();
 * }</pre>
 */
public final class CreateTagInput {

    private final String name;
    private final String color;

    private CreateTagInput(Builder b) {
        this.name = b.name;
        this.color = b.color;
    }

    /**
     * Starts a builder for a {@code CreateTagInput}.
     *
     * @param name the tag display name (required)
     * @return a new {@link Builder}
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** {@return the tag display name} */
    public String getName() {
        return name;
    }

    /** {@return the requested tag colour, or {@code null} for the server default} */
    public String getColor() {
        return color;
    }

    /** Fluent builder for {@link CreateTagInput}. */
    public static final class Builder {
        private final String name;
        private String color;

        private Builder(String name) {
            this.name = name;
        }

        /**
         * Sets the tag colour. The server validates it against its palette and
         * defaults to {@code teal} when omitted.
         *
         * @param value the colour
         * @return this builder
         */
        public Builder color(String value) {
            this.color = value;
            return this;
        }

        /**
         * Builds the immutable {@link CreateTagInput}.
         *
         * @return a new {@code CreateTagInput}
         */
        public CreateTagInput build() {
            return new CreateTagInput(this);
        }
    }
}
