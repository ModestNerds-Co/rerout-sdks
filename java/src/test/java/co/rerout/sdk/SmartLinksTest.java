/*
 * rerout-java — Smart Links surface tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.rerout.sdk.MockApi.MockResponse;
import co.rerout.sdk.MockApi.RecordedRequest;
import co.rerout.sdk.model.AbVariant;
import co.rerout.sdk.model.AbVariantInput;
import co.rerout.sdk.model.BatchCreateLinksResult;
import co.rerout.sdk.model.BatchLinkInput;
import co.rerout.sdk.model.CreateLinkInput;
import co.rerout.sdk.model.Link;
import co.rerout.sdk.model.RoutingRule;
import co.rerout.sdk.model.UpdateLinkInput;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SmartLinksTest {

    private MockApi api;

    @BeforeEach
    void setUp() {
        api = new MockApi();
    }

    @AfterEach
    void tearDown() {
        api.close();
    }

    // ─── Link response parsing ──────────────────────────────────────────────

    @Test
    void linkParsesSmartLinkFields() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_SMART_LINK_JSON));
        Link link = api.client().links().get("q4");

        assertTrue(link.isPasswordProtected());
        assertEquals(Long.valueOf(1000), link.getMaxClicks());
        assertEquals(42L, link.getClickCount());
        assertTrue(link.isTrackConversions());

        assertEquals(1, link.getRoutingRules().size());
        RoutingRule rule = link.getRoutingRules().get(0);
        assertEquals("country", rule.getConditionType());
        assertEquals("is", rule.getConditionOp());
        assertEquals("ZA", rule.getConditionValue());
        assertEquals("https://example.com/za", rule.getTargetUrl());

        assertEquals(2, link.getAbVariants().size());
        AbVariant first = link.getAbVariants().get(0);
        assertEquals(1, first.getId());
        assertEquals("https://example.com/a", first.getTargetUrl());
        assertEquals(70, first.getWeight());
    }

    @Test
    void linkDefaultsSmartLinkFieldsWhenAbsent() {
        // The classic (pre-Smart-Links) payload has none of the new fields.
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_NO_TAGS_JSON));
        Link link = api.client().links().get("q4");

        assertFalse(link.isPasswordProtected());
        assertNull(link.getMaxClicks());
        assertEquals(0L, link.getClickCount());
        assertFalse(link.isTrackConversions());
        assertTrue(link.getRoutingRules().isEmpty());
        assertTrue(link.getAbVariants().isEmpty());
    }

    // ─── CreateLinkInput serialization ──────────────────────────────────────

    @Test
    void createOmitsSmartLinkFieldsWhenUnset() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_SMART_LINK_JSON));
        api.client().links().create(CreateLinkInput.builder("https://example.com").build());

        String body = api.takeRequest().body;
        assertFalse(body.contains("password"));
        assertFalse(body.contains("max_clicks"));
        assertFalse(body.contains("track_conversions"));
        assertFalse(body.contains("routing_rules"));
        assertFalse(body.contains("ab_variants"));
    }

    @Test
    void createSerializesSmartLinkFields() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_SMART_LINK_JSON));
        api.client().links().create(
                CreateLinkInput.builder("https://example.com")
                        .password("s3cret")
                        .maxClicks(500)
                        .trackConversions(true)
                        .routingRules(new RoutingRule("device", "in", "mobile,tablet",
                                "https://example.com/m"))
                        .abVariants(
                                new AbVariantInput("https://example.com/a", 60),
                                new AbVariantInput("https://example.com/b"))
                        .build());

        String body = api.takeRequest().body;
        assertTrue(body.contains("\"password\":\"s3cret\""));
        assertTrue(body.contains("\"max_clicks\":500"));
        assertTrue(body.contains("\"track_conversions\":true"));
        assertTrue(body.contains("\"condition_type\":\"device\""));
        assertTrue(body.contains("\"condition_op\":\"in\""));
        assertTrue(body.contains("\"condition_value\":\"mobile,tablet\""));
        assertTrue(body.contains("\"target_url\":\"https://example.com/m\""));
        assertTrue(body.contains("\"target_url\":\"https://example.com/a\""));
        assertTrue(body.contains("\"weight\":60"));
        // The second variant omits weight (server default).
        assertTrue(body.contains("\"target_url\":\"https://example.com/b\""));
    }

    // ─── UpdateLinkInput serialization ──────────────────────────────────────

    @Test
    void updateSerializesSmartLinkFields() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_SMART_LINK_JSON));
        api.client().links().update("q4",
                UpdateLinkInput.builder()
                        .password("newpass")
                        .maxClicks(250)
                        .trackConversions(true)
                        .routingRules(new RoutingRule("country", "is_not", "US",
                                "https://example.com/intl"))
                        .abVariants(new AbVariantInput("https://example.com/a", 50))
                        .build());

        String body = api.takeRequest().body;
        assertTrue(body.contains("\"password\":\"newpass\""));
        assertTrue(body.contains("\"max_clicks\":250"));
        assertTrue(body.contains("\"track_conversions\":true"));
        assertTrue(body.contains("\"condition_type\":\"country\""));
        assertTrue(body.contains("\"target_url\":\"https://example.com/a\""));
    }

    @Test
    void updateClearsPasswordAndMaxClicks() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_SMART_LINK_JSON));
        api.client().links().update("q4",
                UpdateLinkInput.builder()
                        .clearPassword()
                        .clearMaxClicks()
                        .build());

        String body = api.takeRequest().body;
        assertTrue(body.contains("\"password\":null"));
        assertTrue(body.contains("\"max_clicks\":null"));
    }

    @Test
    void updateAllowsEmptyRoutingRulesAsFullReplace() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_SMART_LINK_JSON));
        api.client().links().update("q4",
                UpdateLinkInput.builder().routingRules(List.of()).build());

        String body = api.takeRequest().body;
        assertTrue(body.contains("\"routing_rules\":[]"));
    }

    @Test
    void updateLeavesSmartLinkFieldsAloneByDefault() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_SMART_LINK_JSON));
        api.client().links().update("q4", UpdateLinkInput.builder().active(true).build());

        String body = api.takeRequest().body;
        assertEquals("{\"is_active\":true}", body);
    }

    @Test
    void updateIsEmptyAccountsForSmartLinkFields() {
        assertFalse(UpdateLinkInput.builder().password("x").build().isEmpty());
        assertFalse(UpdateLinkInput.builder().maxClicks(10).build().isEmpty());
        assertFalse(UpdateLinkInput.builder().trackConversions(true).build().isEmpty());
        assertFalse(UpdateLinkInput.builder().routingRules(List.of()).build().isEmpty());
        assertFalse(UpdateLinkInput.builder().abVariants(List.of()).build().isEmpty());
    }

    // ─── createBatch ────────────────────────────────────────────────────────

    @Test
    void createBatchPostsToBatchEndpoint() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_BATCH_RESULT_JSON));
        api.client().links().createBatch(List.of(
                BatchLinkInput.builder("https://example.com/a").build(),
                BatchLinkInput.builder("https://example.com/b").build()));

        RecordedRequest recorded = api.takeRequest();
        assertEquals("POST", recorded.method);
        assertEquals("/v1/links/batch", recorded.path);
    }

    @Test
    void createBatchSerializesLinksArray() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_BATCH_RESULT_JSON));
        api.client().links().createBatch(List.of(
                BatchLinkInput.builder("https://example.com/a")
                        .code("a")
                        .expiresAt(1900000000L)
                        .domainHostname("go.brand.com")
                        .build(),
                BatchLinkInput.builder("https://example.com/b").build()));

        String body = api.takeRequest().body;
        assertTrue(body.contains("\"links\":["));
        assertTrue(body.contains("\"target_url\":\"https://example.com/a\""));
        assertTrue(body.contains("\"code\":\"a\""));
        assertTrue(body.contains("\"expires_at\":1900000000"));
        assertTrue(body.contains("\"domain_hostname\":\"go.brand.com\""));
        assertTrue(body.contains("\"target_url\":\"https://example.com/b\""));
    }

    @Test
    void createBatchParsesResult() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_BATCH_RESULT_JSON));
        BatchCreateLinksResult result = api.client().links().createBatch(List.of(
                BatchLinkInput.builder("https://example.com/a").build(),
                BatchLinkInput.builder("https://bad").build()));

        assertEquals(1, result.getCreated());
        assertEquals(2, result.getTotal());
        assertEquals(2, result.getResults().size());

        assertEquals(0, result.getResults().get(0).getIndex());
        assertTrue(result.getResults().get(0).isOk());
        assertEquals("aaa", result.getResults().get(0).getCode());
        assertNull(result.getResults().get(0).getError());

        assertEquals(1, result.getResults().get(1).getIndex());
        assertFalse(result.getResults().get(1).isOk());
        assertNull(result.getResults().get(1).getCode());
        assertEquals("bad_target_url", result.getResults().get(1).getError());
    }

    @Test
    void createBatchSurfacesAServerError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"rate_limited\",\"message\":\"slow down\"}", 429));
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().links().createBatch(List.of(
                        BatchLinkInput.builder("https://example.com/a").build())));
        assertEquals("rate_limited", ex.getCode());
        assertEquals(429, ex.getStatus());
    }

    @Test
    void createBatchAsyncCompletesWithTheParsedResult() throws Exception {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_BATCH_RESULT_JSON));
        BatchCreateLinksResult result = api.client().links().createBatchAsync(List.of(
                BatchLinkInput.builder("https://example.com/a").build())).get();
        assertEquals(1, result.getCreated());
    }
}
