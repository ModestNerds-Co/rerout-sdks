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
import co.rerout.sdk.model.CreateLinkInput;
import co.rerout.sdk.model.DeleteResult;
import co.rerout.sdk.model.Link;
import co.rerout.sdk.model.LinkStats;
import co.rerout.sdk.model.ListLinksResult;
import co.rerout.sdk.model.UpdateLinkInput;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Link operations namespace — create, list, get, update, delete, and stats.
 * Reached via {@link Rerout#links()}.
 *
 * <p>Every operation ships in two forms: a blocking method that returns the
 * value directly and throws {@link ReroutException} on failure, and an
 * {@code …Async} method that returns a {@link CompletableFuture} which
 * completes exceptionally with {@link ReroutException}. The async form is the
 * primary path; the blocking form joins it.
 */
public final class Links {

    private final HttpTransport transport;

    Links(HttpTransport transport) {
        this.transport = transport;
    }

    // ─── create ─────────────────────────────────────────────────────────────

    /**
     * Creates a new short link.
     *
     * @param input the link specification
     * @return the created link
     * @throws ReroutException on any failure
     */
    public Link create(CreateLinkInput input) {
        return join(createAsync(input));
    }

    /**
     * Creates a new short link asynchronously.
     *
     * @param input the link specification
     * @return a future of the created link
     */
    public CompletableFuture<Link> createAsync(CreateLinkInput input) {
        String path = "/v1/links";
        String body = transport.gson().toJson(input);
        return transport
                .executeAsync(RequestSpec.withBody(Method.POST, path, body))
                .thenApply(text -> transport.decode(text, Link.class, path));
    }

    // ─── list ───────────────────────────────────────────────────────────────

    /**
     * Lists the first page of links in the project.
     *
     * @return a page of links
     * @throws ReroutException on any failure
     */
    public ListLinksResult list() {
        return list(null, null);
    }

    /**
     * Lists a paginated page of links in the project.
     *
     * @param cursor the pagination cursor from a previous call, or {@code null}
     * @param limit  the page size, or {@code null} for the server default
     * @return a page of links
     * @throws ReroutException on any failure
     */
    public ListLinksResult list(Long cursor, Integer limit) {
        return join(listAsync(cursor, limit));
    }

    /**
     * Lists the first page of links asynchronously.
     *
     * @return a future of a page of links
     */
    public CompletableFuture<ListLinksResult> listAsync() {
        return listAsync(null, null);
    }

    /**
     * Lists a paginated page of links asynchronously.
     *
     * @param cursor the pagination cursor from a previous call, or {@code null}
     * @param limit  the page size, or {@code null} for the server default
     * @return a future of a page of links
     */
    public CompletableFuture<ListLinksResult> listAsync(Long cursor, Integer limit) {
        String path = "/v1/links";
        Map<String, String> query = new LinkedHashMap<>();
        if (cursor != null) {
            query.put("cursor", String.valueOf(cursor));
        }
        if (limit != null) {
            query.put("limit", String.valueOf(limit));
        }
        return transport
                .executeAsync(RequestSpec.of(Method.GET, path, query))
                .thenApply(text -> transport.decode(text, ListLinksResult.class, path));
    }

    // ─── get ────────────────────────────────────────────────────────────────

    /**
     * Gets a single link by code.
     *
     * @param code the short link code
     * @return the link
     * @throws ReroutException on any failure
     */
    public Link get(String code) {
        return join(getAsync(code));
    }

    /**
     * Gets a single link by code asynchronously.
     *
     * @param code the short link code
     * @return a future of the link
     */
    public CompletableFuture<Link> getAsync(String code) {
        String path = "/v1/links/" + HttpTransport.encodePathSegment(code);
        return transport
                .executeAsync(RequestSpec.of(Method.GET, path))
                .thenApply(text -> transport.decode(text, Link.class, path));
    }

    // ─── update ─────────────────────────────────────────────────────────────

    /**
     * Patches a link. Only the fields set on {@code input} are sent. An empty
     * patch is rejected client-side with code {@code bad_request} without
     * hitting the API.
     *
     * @param code  the short link code
     * @param input the patch to apply
     * @return the updated link
     * @throws ReroutException on any failure, including an empty patch
     */
    public Link update(String code, UpdateLinkInput input) {
        return join(updateAsync(code, input));
    }

    /**
     * Patches a link asynchronously. Only the fields set on {@code input} are
     * sent. An empty patch completes the future exceptionally with code
     * {@code bad_request} without hitting the API.
     *
     * @param code  the short link code
     * @param input the patch to apply
     * @return a future of the updated link
     */
    public CompletableFuture<Link> updateAsync(String code, UpdateLinkInput input) {
        String path = "/v1/links/" + HttpTransport.encodePathSegment(code);
        Map<String, Object> patch = input.toJsonMap();
        if (patch.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new ReroutException(
                            "bad_request",
                            "UpdateLinkInput has no fields to send.",
                            0,
                            path));
        }
        // The null-safe codec emits explicit `null` for cleared fields; the
        // patch map only ever contains the keys the caller actually set.
        String body = transport.nullSafeGson().toJson(patch);
        return transport
                .executeAsync(RequestSpec.withBody(Method.PATCH, path, body))
                .thenApply(text -> transport.decode(text, Link.class, path));
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a link. The short URL stops redirecting and is gone from
     * lists.
     *
     * @param code the short link code
     * @return the delete result
     * @throws ReroutException on any failure
     */
    public DeleteResult delete(String code) {
        return join(deleteAsync(code));
    }

    /**
     * Soft-deletes a link asynchronously.
     *
     * @param code the short link code
     * @return a future of the delete result
     */
    public CompletableFuture<DeleteResult> deleteAsync(String code) {
        String path = "/v1/links/" + HttpTransport.encodePathSegment(code);
        return transport
                .executeAsync(RequestSpec.of(Method.DELETE, path))
                .thenApply(text -> {
                    if (text == null || text.isEmpty()) {
                        return new DeleteResult(true);
                    }
                    return transport.decode(text, DeleteResult.class, path);
                });
    }

    // ─── stats ──────────────────────────────────────────────────────────────

    /**
     * Returns per-link click stats over the last 30 days.
     *
     * @param code the short link code
     * @return the link stats
     * @throws ReroutException on any failure
     */
    public LinkStats stats(String code) {
        return stats(code, 30);
    }

    /**
     * Returns per-link click stats over the given window.
     *
     * @param code the short link code
     * @param days the window size in days
     * @return the link stats
     * @throws ReroutException on any failure
     */
    public LinkStats stats(String code, int days) {
        return join(statsAsync(code, days));
    }

    /**
     * Returns per-link click stats over the last 30 days asynchronously.
     *
     * @param code the short link code
     * @return a future of the link stats
     */
    public CompletableFuture<LinkStats> statsAsync(String code) {
        return statsAsync(code, 30);
    }

    /**
     * Returns per-link click stats over the given window asynchronously.
     *
     * @param code the short link code
     * @param days the window size in days
     * @return a future of the link stats
     */
    public CompletableFuture<LinkStats> statsAsync(String code, int days) {
        String path = "/v1/links/" + HttpTransport.encodePathSegment(code) + "/stats";
        Map<String, String> query = new LinkedHashMap<>();
        query.put("days", String.valueOf(days));
        return transport
                .executeAsync(RequestSpec.of(Method.GET, path, query))
                .thenApply(text -> transport.decode(text, LinkStats.class, path));
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw HttpTransport.unwrap(e, null);
        }
    }
}
