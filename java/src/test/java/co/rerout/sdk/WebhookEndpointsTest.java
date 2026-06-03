/*
 * rerout-java — webhook endpoint management namespace tests.
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
import co.rerout.sdk.model.CreateWebhookInput;
import co.rerout.sdk.model.CreatedWebhook;
import co.rerout.sdk.model.DeleteResult;
import co.rerout.sdk.model.ListWebhooksResult;
import co.rerout.sdk.model.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WebhookEndpointsTest {

    private MockApi api;

    @BeforeEach
    void setUp() {
        api = new MockApi();
    }

    @AfterEach
    void tearDown() {
        api.close();
    }

    private static CreateWebhookInput sampleInput() {
        return CreateWebhookInput
                .builder("Order events", "https://example.com/hooks/rerout")
                .events("link.created", "link.clicked")
                .build();
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Test
    void createPostsToTheWebhooksEndpointAndReturnsTheSigningSecret() {
        api.enqueue(MockResponse.json(
                "{\"endpoint\":" + MockApi.SAMPLE_WEBHOOK_JSON + ","
                + "\"signing_secret\":\"whsec_supersecret\"}", 201));
        CreatedWebhook result = api.client().webhooks().create(sampleInput());
        assertEquals("whsec_supersecret", result.getSigningSecret());
        assertEquals("wh_abc123", result.getEndpoint().getId());
        RecordedRequest recorded = api.takeRequest();
        assertEquals("POST", recorded.method);
        assertEquals("/v1/projects/me/webhooks", recorded.path);
    }

    @Test
    void createSendsTheRequiredFieldsAndOmitsUnsetOptionals() {
        api.enqueue(MockResponse.json(
                "{\"endpoint\":" + MockApi.SAMPLE_WEBHOOK_JSON + ","
                + "\"signing_secret\":\"whsec_x\"}", 201));
        api.client().webhooks().create(sampleInput());
        String body = api.takeRequest().body;
        assertTrue(body.contains("\"name\":\"Order events\""));
        assertTrue(body.contains("\"url\":\"https://example.com/hooks/rerout\""));
        assertTrue(body.contains("\"events\":[\"link.created\",\"link.clicked\"]"));
        assertFalse(body.contains("is_active"));
        assertFalse(body.contains("payload_format"));
    }

    @Test
    void createForwardsIsActiveAndPayloadFormatWhenProvided() {
        api.enqueue(MockResponse.json(
                "{\"endpoint\":" + MockApi.SAMPLE_WEBHOOK_JSON + ","
                + "\"signing_secret\":\"whsec_x\"}", 201));
        api.client().webhooks().create(
                CreateWebhookInput
                        .builder("Slack", "https://hooks.slack.com/services/T/B/x")
                        .events("link.created")
                        .isActive(false)
                        .payloadFormat("slack")
                        .build());
        String body = api.takeRequest().body;
        assertTrue(body.contains("\"is_active\":false"));
        assertTrue(body.contains("\"payload_format\":\"slack\""));
    }

    @Test
    void createParsesTheEndpointFields() {
        api.enqueue(MockResponse.json(
                "{\"endpoint\":" + MockApi.SAMPLE_WEBHOOK_JSON + ","
                + "\"signing_secret\":\"whsec_x\"}", 201));
        Webhook endpoint = api.client().webhooks().create(sampleInput()).getEndpoint();
        assertEquals("prj_test", endpoint.getProjectId());
        assertEquals("https://example.com/hooks/rerout", endpoint.getUrl());
        assertEquals(2, endpoint.getEvents().size());
        assertEquals("link.created", endpoint.getEvents().get(0));
        assertEquals("json", endpoint.getPayloadFormat());
        assertTrue(endpoint.isActive());
        assertEquals(1700000000L, endpoint.getCreatedAt());
        assertEquals(null, endpoint.getLastDeliveryAt());
    }

    @Test
    void createSurfacesAServerError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"bad_request\",\"message\":\"bad url\"}", 422));
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().webhooks().create(sampleInput()));
        assertEquals("bad_request", ex.getCode());
        assertEquals(422, ex.getStatus());
    }

    @Test
    void createAsyncCompletesWithTheParsedResult() throws Exception {
        api.enqueue(MockResponse.json(
                "{\"endpoint\":" + MockApi.SAMPLE_WEBHOOK_JSON + ","
                + "\"signing_secret\":\"whsec_async\"}", 201));
        CreatedWebhook result = api.client().webhooks().createAsync(sampleInput()).get();
        assertEquals("whsec_async", result.getSigningSecret());
    }

    // ─── list ───────────────────────────────────────────────────────────────

    @Test
    void listGetsTheWebhooksEndpointAndParsesEndpointsAndEventTypes() {
        api.enqueue(MockResponse.json(
                "{\"endpoints\":[" + MockApi.SAMPLE_WEBHOOK_JSON + "],"
                + "\"event_types\":[\"link.created\",\"link.clicked\",\"domain.verified\"]}"));
        ListWebhooksResult result = api.client().webhooks().list();
        assertEquals(1, result.getEndpoints().size());
        assertEquals("https://example.com/hooks/rerout",
                result.getEndpoints().get(0).getUrl());
        assertTrue(result.getEventTypes().contains("domain.verified"));
        RecordedRequest recorded = api.takeRequest();
        assertEquals("GET", recorded.method);
        assertEquals("/v1/projects/me/webhooks", recorded.path);
    }

    @Test
    void listToleratesAnEmptyEndpointList() {
        api.enqueue(MockResponse.json(
                "{\"endpoints\":[],\"event_types\":[]}"));
        ListWebhooksResult result = api.client().webhooks().list();
        assertTrue(result.getEndpoints().isEmpty());
        assertTrue(result.getEventTypes().isEmpty());
    }

    @Test
    void listSurfacesAnAuthError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"unauthorized\",\"message\":\"bad key\"}", 401));
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().webhooks().list());
        assertEquals("unauthorized", ex.getCode());
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Test
    void deleteSendsDeleteToTheEndpointPathAndReturnsTheDeletedFlag() {
        api.enqueue(MockResponse.json("{\"deleted\":true}"));
        DeleteResult result = api.client().webhooks().delete("wh_abc123");
        assertTrue(result.isDeleted());
        RecordedRequest recorded = api.takeRequest();
        assertEquals("DELETE", recorded.method);
        assertEquals("/v1/projects/me/webhooks/wh_abc123", recorded.path);
    }

    @Test
    void deleteToleratesAnEmptySuccessBody() {
        api.enqueue(MockResponse.status(204));
        assertTrue(api.client().webhooks().delete("wh_abc123").isDeleted());
    }

    @Test
    void deleteSurfacesAForbiddenError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"forbidden\",\"message\":\"not yours\"}", 403));
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().webhooks().delete("wh_abc123"));
        assertEquals("forbidden", ex.getCode());
    }

    // ─── URL encoding edge cases ────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "wh_abc123, /v1/projects/me/webhooks/wh_abc123",
        "wh a/b, /v1/projects/me/webhooks/wh%20a%2Fb",
        "wh+x, /v1/projects/me/webhooks/wh%2Bx",
    })
    void endpointIdsAreUrlEncodedAsASinglePathSegment(String id, String expectedPath) {
        api.enqueue(MockResponse.json("{\"deleted\":true}"));
        api.client().webhooks().delete(id);
        assertEquals(expectedPath, api.takeRequest().path);
    }
}
