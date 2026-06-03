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
import co.rerout.sdk.model.CreateWebhookInput;
import co.rerout.sdk.model.CreatedWebhook;
import co.rerout.sdk.model.DeleteResult;
import co.rerout.sdk.model.ListWebhooksResult;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Webhook endpoint management namespace — create, list, and delete the
 * endpoints the project delivers events to. Reached via
 * {@link Rerout#webhooks()}.
 *
 * <p>This is the <em>management</em> surface. The separate
 * {@link Webhooks} class is the inbound <em>signature verifier</em> for received
 * deliveries; the two are independent.
 *
 * <p>Every operation ships in two forms: a blocking method that returns the
 * value directly and throws {@link ReroutException} on failure, and an
 * {@code …Async} method that returns a {@link CompletableFuture} which completes
 * exceptionally with {@link ReroutException}. The async form is the primary
 * path; the blocking form joins it. Mirrors {@link Links}.
 */
public final class WebhookEndpoints {

    private static final String PATH = "/v1/projects/me/webhooks";

    private final HttpTransport transport;

    WebhookEndpoints(HttpTransport transport) {
        this.transport = transport;
    }

    // ─── create ─────────────────────────────────────────────────────────────

    /**
     * Creates a webhook endpoint for the project that owns the API key.
     *
     * <p>The returned {@code signingSecret} is shown once — persist it to verify
     * deliveries with {@link Webhooks#verifyReroutSignature}.
     *
     * @param input the endpoint specification
     * @return the created endpoint and its one-time signing secret
     * @throws ReroutException on any failure
     */
    public CreatedWebhook create(CreateWebhookInput input) {
        return join(createAsync(input));
    }

    /**
     * Creates a webhook endpoint asynchronously.
     *
     * @param input the endpoint specification
     * @return a future of the created endpoint and its one-time signing secret
     */
    public CompletableFuture<CreatedWebhook> createAsync(CreateWebhookInput input) {
        String body = transport.gson().toJson(input);
        return transport
                .executeAsync(RequestSpec.withBody(Method.POST, PATH, body))
                .thenApply(text -> transport.decode(text, CreatedWebhook.class, PATH));
    }

    // ─── list ───────────────────────────────────────────────────────────────

    /**
     * Lists the webhook endpoints registered to the project and the event types
     * the server can deliver.
     *
     * @return the endpoints and supported event types
     * @throws ReroutException on any failure
     */
    public ListWebhooksResult list() {
        return join(listAsync());
    }

    /**
     * Lists the webhook endpoints and supported event types asynchronously.
     *
     * @return a future of the endpoints and supported event types
     */
    public CompletableFuture<ListWebhooksResult> listAsync() {
        return transport
                .executeAsync(RequestSpec.of(Method.GET, PATH))
                .thenApply(text -> transport.decode(text, ListWebhooksResult.class, PATH));
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a webhook endpoint and abandons its pending deliveries.
     * Idempotent.
     *
     * @param endpointId the endpoint identifier ({@code wh_…})
     * @return the delete result
     * @throws ReroutException on any failure
     */
    public DeleteResult delete(String endpointId) {
        return join(deleteAsync(endpointId));
    }

    /**
     * Soft-deletes a webhook endpoint asynchronously.
     *
     * @param endpointId the endpoint identifier ({@code wh_…})
     * @return a future of the delete result
     */
    public CompletableFuture<DeleteResult> deleteAsync(String endpointId) {
        String path = PATH + "/" + HttpTransport.encodePathSegment(endpointId);
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
