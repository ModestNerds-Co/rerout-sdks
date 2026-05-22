/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

import co.rerout.sdk.internal.HttpTransport;
import co.rerout.sdk.internal.HttpTransport.Method;
import co.rerout.sdk.internal.RequestSpec;
import co.rerout.sdk.model.ProjectInfo;
import co.rerout.sdk.model.ProjectStats;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Project-level operations namespace — aggregate stats and project info.
 * Reached via {@link Rerout#project()}.
 *
 * <p>Each operation ships in a blocking and an {@code …Async} form, mirroring
 * {@link Links}.
 */
public final class Project {

    private final HttpTransport transport;

    Project(HttpTransport transport) {
        this.transport = transport;
    }

    // ─── stats ──────────────────────────────────────────────────────────────

    /**
     * Returns aggregate stats across every link in the project over the last
     * 30 days.
     *
     * @return the project stats
     * @throws ReroutException on any failure
     */
    public ProjectStats stats() {
        return stats(30);
    }

    /**
     * Returns aggregate stats across every link in the project over the given
     * window.
     *
     * @param days the window size in days
     * @return the project stats
     * @throws ReroutException on any failure
     */
    public ProjectStats stats(int days) {
        return join(statsAsync(days));
    }

    /**
     * Returns aggregate project stats over the last 30 days asynchronously.
     *
     * @return a future of the project stats
     */
    public CompletableFuture<ProjectStats> statsAsync() {
        return statsAsync(30);
    }

    /**
     * Returns aggregate project stats over the given window asynchronously.
     *
     * @param days the window size in days
     * @return a future of the project stats
     */
    public CompletableFuture<ProjectStats> statsAsync(int days) {
        String path = "/v1/projects/me/stats";
        Map<String, String> query = new LinkedHashMap<>();
        query.put("days", String.valueOf(days));
        return transport
                .executeAsync(RequestSpec.of(Method.GET, path, query))
                .thenApply(text -> transport.decode(text, ProjectStats.class, path));
    }

    // ─── me ─────────────────────────────────────────────────────────────────

    /**
     * Returns info about the project that owns the current API key.
     *
     * @return the project info
     * @throws ReroutException on any failure
     */
    public ProjectInfo me() {
        return join(meAsync());
    }

    /**
     * Returns info about the project that owns the current API key
     * asynchronously.
     *
     * @return a future of the project info
     */
    public CompletableFuture<ProjectInfo> meAsync() {
        String path = "/v1/projects/me";
        return transport
                .executeAsync(RequestSpec.of(Method.GET, path))
                .thenApply(text -> transport.decode(text, ProjectInfo.class, path));
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw HttpTransport.unwrap(e, null);
        }
    }
}
