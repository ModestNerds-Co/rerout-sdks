/*
 * rerout-java — conversion tracking namespace tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.rerout.sdk.MockApi.MockResponse;
import co.rerout.sdk.MockApi.RecordedRequest;
import co.rerout.sdk.model.RecordConversionInput;
import co.rerout.sdk.model.RecordedConversion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversionsTest {

    private MockApi api;

    @BeforeEach
    void setUp() {
        api = new MockApi();
    }

    @AfterEach
    void tearDown() {
        api.close();
    }

    @Test
    void recordPostsToTheConversionsEndpoint() {
        api.enqueue(MockResponse.json("{\"recorded\":true,\"duplicate\":false}"));
        RecordedConversion result = api.client().conversions().record(
                RecordConversionInput.builder("clk_123", "purchase").build());

        assertTrue(result.isRecorded());
        assertFalse(result.isDuplicate());

        RecordedRequest recorded = api.takeRequest();
        assertEquals("POST", recorded.method);
        assertEquals("/v1/conversions", recorded.path);
    }

    @Test
    void recordSendsRequiredFieldsAndOmitsUnsetOptionals() {
        api.enqueue(MockResponse.json("{\"recorded\":true,\"duplicate\":false}"));
        api.client().conversions().record(
                RecordConversionInput.builder("clk_123", "signup").build());

        String body = api.takeRequest().body;
        assertTrue(body.contains("\"click_id\":\"clk_123\""));
        assertTrue(body.contains("\"event_name\":\"signup\""));
        assertFalse(body.contains("value_cents"));
        assertFalse(body.contains("currency"));
    }

    @Test
    void recordForwardsValueAndCurrencyWhenProvided() {
        api.enqueue(MockResponse.json("{\"recorded\":true,\"duplicate\":false}"));
        api.client().conversions().record(
                RecordConversionInput.builder("clk_123", "purchase")
                        .valueCents(4999)
                        .currency("USD")
                        .build());

        String body = api.takeRequest().body;
        assertTrue(body.contains("\"value_cents\":4999"));
        assertTrue(body.contains("\"currency\":\"USD\""));
    }

    @Test
    void recordParsesTheDuplicateFlag() {
        api.enqueue(MockResponse.json("{\"recorded\":true,\"duplicate\":true}"));
        RecordedConversion result = api.client().conversions().record(
                RecordConversionInput.builder("clk_123", "purchase").build());

        assertTrue(result.isRecorded());
        assertTrue(result.isDuplicate());
    }

    @Test
    void recordSurfacesAServerError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"not_found\",\"message\":\"unknown click\"}", 404));
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().conversions().record(
                        RecordConversionInput.builder("clk_missing", "purchase").build()));
        assertEquals("not_found", ex.getCode());
        assertEquals(404, ex.getStatus());
    }

    @Test
    void recordAsyncCompletesWithTheParsedResult() throws Exception {
        api.enqueue(MockResponse.json("{\"recorded\":true,\"duplicate\":false}"));
        RecordedConversion result = api.client().conversions().recordAsync(
                RecordConversionInput.builder("clk_123", "purchase").build()).get();
        assertTrue(result.isRecorded());
    }
}
