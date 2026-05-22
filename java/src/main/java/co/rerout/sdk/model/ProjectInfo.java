/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

/** Info about the project that owns the current API key. */
public final class ProjectInfo {

    private final String id;
    private final String name;
    private final String slug;

    /**
     * Creates a {@code ProjectInfo}.
     *
     * @param id   the project identifier
     * @param name the human-readable project name
     * @param slug the URL-safe project slug
     */
    public ProjectInfo(String id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }

    /** {@return the project identifier} */
    public String getId() {
        return id;
    }

    /** {@return the human-readable project name} */
    public String getName() {
        return name;
    }

    /** {@return the URL-safe project slug} */
    public String getSlug() {
        return slug;
    }
}
