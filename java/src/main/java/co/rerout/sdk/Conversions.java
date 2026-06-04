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
import co.rerout.sdk.model.RecordConversionInput;
import co.rerout.sdk.model.RecordedConversion;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Conversion tracking namespace — record conversion events against a click so
 * attribution and value can be reported in analytics. Reached via
 * {@link Rerout#conversions()}.
 *
 * <p>Every operation ships in two forms: a blocking method that returns the
 * value directly and throws {@link ReroutException} on failure, and an
 * {@code …Async} method that returns a {@link CompletableFuture} which completes
 * exceptionally with {@link ReroutException}. The async form is the primary
 * path; the blocking form joins it. Mirrors {@link Links}.
 */
public final class Conversions {

    private static final String PATH = "/v1/conversions";

    private final HttpTransport transport;

    Conversions(HttpTransport transport) {
        this.transport = transport;
    }

    /**
     * Records a conversion event for a click. Idempotent — recording the same
     * click + event twice returns {@code duplicate = true} and keeps the first
     * record.
     *
     * @param input the conversion to record
     * @return the record result
     * @throws ReroutException on any failure
     */
    public RecordedConversion record(RecordConversionInput input) {
        return join(recordAsync(input));
    }

    /**
     * Records a conversion event asynchronously.
     *
     * @param input the conversion to record
     * @return a future of the record result
     */
    public CompletableFuture<RecordedConversion> recordAsync(RecordConversionInput input) {
        String body = transport.gson().toJson(input);
        return transport
                .executeAsync(RequestSpec.withBody(Method.POST, PATH, body))
                .thenApply(text -> transport.decode(text, RecordedConversion.class, PATH));
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw HttpTransport.unwrap(e, null);
        }
    }
}
