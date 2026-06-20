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
import co.rerout.sdk.model.CreateTagInput;
import co.rerout.sdk.model.DeleteResult;
import co.rerout.sdk.model.ListTagsResult;
import co.rerout.sdk.model.Tag;
import co.rerout.sdk.model.UpdateTagInput;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Tag management namespace — list, create, update, and delete the project's
 * tags. Reached via {@link Rerout#tags()}.
 *
 * <p>Tags are project-scoped (resolved from the API key) and hit
 * {@code /v1/projects/me/tags}. {@code list} returns each tag with its live
 * link count; {@code create} and {@code update} return a plain {@link Tag}.
 *
 * <p>Every operation ships in two forms: a blocking method that returns the
 * value directly and throws {@link ReroutException} on failure, and an
 * {@code …Async} method that returns a {@link CompletableFuture} which completes
 * exceptionally with {@link ReroutException}. The async form is the primary
 * path; the blocking form joins it. Mirrors {@link Links}.
 */
public final class Tags {

    private static final String PATH = "/v1/projects/me/tags";

    private final HttpTransport transport;

    Tags(HttpTransport transport) {
        this.transport = transport;
    }

    // ─── list ───────────────────────────────────────────────────────────────

    /**
     * Lists the project's tags, each with the number of live links it is
     * attached to.
     *
     * @return the tags with their link counts
     * @throws ReroutException on any failure
     */
    public ListTagsResult list() {
        return join(listAsync());
    }

    /**
     * Lists the project's tags asynchronously.
     *
     * @return a future of the tags with their link counts
     */
    public CompletableFuture<ListTagsResult> listAsync() {
        return transport
                .executeAsync(RequestSpec.of(Method.GET, PATH))
                .thenApply(text -> transport.decode(text, ListTagsResult.class, PATH));
    }

    // ─── create ─────────────────────────────────────────────────────────────

    /**
     * Creates a tag. {@code color} is optional; the server defaults it.
     *
     * @param input the tag specification
     * @return the created tag
     * @throws ReroutException on any failure
     */
    public Tag create(CreateTagInput input) {
        return join(createAsync(input));
    }

    /**
     * Creates a tag asynchronously.
     *
     * @param input the tag specification
     * @return a future of the created tag
     */
    public CompletableFuture<Tag> createAsync(CreateTagInput input) {
        String body = transport.gson().toJson(input);
        return transport
                .executeAsync(RequestSpec.withBody(Method.POST, PATH, body))
                .thenApply(text -> transport.decode(text, Tag.class, PATH));
    }

    // ─── update ─────────────────────────────────────────────────────────────

    /**
     * Updates a tag's name and/or color. Only the fields set on {@code input}
     * are sent; an unset field is left unchanged. An empty patch is rejected
     * client-side with code {@code bad_request} without hitting the API,
     * matching {@link Links#update}.
     *
     * @param tagId the tag identifier ({@code tag_…})
     * @param input the patch to apply
     * @return the updated tag
     * @throws ReroutException on any failure, including an empty patch
     */
    public Tag update(String tagId, UpdateTagInput input) {
        return join(updateAsync(tagId, input));
    }

    /**
     * Updates a tag asynchronously. Only the fields set on {@code input} are
     * sent. An empty patch completes the future exceptionally with code
     * {@code bad_request} without hitting the API.
     *
     * @param tagId the tag identifier ({@code tag_…})
     * @param input the patch to apply
     * @return a future of the updated tag
     */
    public CompletableFuture<Tag> updateAsync(String tagId, UpdateTagInput input) {
        String path = PATH + "/" + HttpTransport.encodePathSegment(tagId);
        Map<String, Object> patch = input.toJsonMap();
        if (patch.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new ReroutException(
                            "bad_request",
                            "UpdateTagInput has no fields to send.",
                            0,
                            path));
        }
        String body = transport.gson().toJson(patch);
        return transport
                .executeAsync(RequestSpec.withBody(Method.PATCH, path, body))
                .thenApply(text -> transport.decode(text, Tag.class, path));
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    /**
     * Deletes a tag and drops its assignments from all links.
     *
     * @param tagId the tag identifier ({@code tag_…})
     * @return the delete result
     * @throws ReroutException on any failure
     */
    public DeleteResult delete(String tagId) {
        return join(deleteAsync(tagId));
    }

    /**
     * Deletes a tag asynchronously.
     *
     * @param tagId the tag identifier ({@code tag_…})
     * @return a future of the delete result
     */
    public CompletableFuture<DeleteResult> deleteAsync(String tagId) {
        String path = PATH + "/" + HttpTransport.encodePathSegment(tagId);
        return transport
                .executeAsync(RequestSpec.of(Method.DELETE, path))
                .thenApply(text -> {
                    if (text == null || text.isEmpty()) {
                        return new DeleteResult(true);
                    }
                    return transport.decode(text, DeleteResult.class, path);
                });
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw HttpTransport.unwrap(e, null);
        }
    }
}
