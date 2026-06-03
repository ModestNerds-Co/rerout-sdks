/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

/**
 * Official Java SDK for the <a href="https://rerout.co">Rerout</a> branded-link
 * API.
 *
 * <p>Entry point: {@link co.rerout.sdk.Rerout}. Construct a client with
 * {@link co.rerout.sdk.Rerout#create(String)} or
 * {@link co.rerout.sdk.Rerout#builder(String)}, then reach the
 * {@link co.rerout.sdk.Links}, {@link co.rerout.sdk.Project},
 * {@link co.rerout.sdk.Qr}, and {@link co.rerout.sdk.WebhookEndpoints}
 * namespaces.
 *
 * <p>Every network operation ships in a blocking form (returns the value,
 * throws {@link co.rerout.sdk.ReroutException}) and an async form (returns a
 * {@link java.util.concurrent.CompletableFuture}). Webhook signatures are
 * verified with {@link co.rerout.sdk.Webhooks}.
 *
 * <p>The SDK is pure Java 17 — its only runtime dependency is Gson.
 */
package co.rerout.sdk;
