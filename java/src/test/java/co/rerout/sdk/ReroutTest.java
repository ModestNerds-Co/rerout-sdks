/*
 * rerout-java — client construction and transport tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.rerout.sdk.MockApi.MockResponse;
import co.rerout.sdk.MockApi.RecordedRequest;
import co.rerout.sdk.model.CreateLinkInput;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReroutTest {

    private MockApi api;

    @BeforeEach
    void setUp() {
        api = new MockApi();
    }

    @AfterEach
    void tearDown() {
        api.close();
    }

    // ─── Constructor ────────────────────────────────────────────────────────

    @Test
    void constructorRejectsBlankApiKey() {
        ReroutException ex = assertThrows(
                ReroutException.class, () -> Rerout.create(""));
        assertEquals("missing_api_key", ex.getCode());
        assertEquals(0, ex.getStatus());
    }

    @Test
    void constructorRejectsWhitespaceOnlyApiKey() {
        ReroutException ex = assertThrows(
                ReroutException.class, () -> Rerout.create("   "));
        assertEquals("missing_api_key", ex.getCode());
    }

    @Test
    void constructorRejectsNullApiKey() {
        assertThrows(ReroutException.class, () -> Rerout.create(null));
    }

    @Test
    void constructorAcceptsAValidApiKey() {
        Rerout rerout = Rerout.create("rrk_live_123");
        assertEquals(Rerout.DEFAULT_BASE_URL, rerout.getBaseUrl());
    }

    @Test
    void defaultBaseUrlIsProduction() {
        assertEquals("https://api.rerout.co", Rerout.create("rrk_test").getBaseUrl());
    }

    @Test
    void baseUrlTrailingSlashesAreTrimmed() {
        Rerout rerout = Rerout.builder("rrk_test")
                .baseUrl("https://api.staging.rerout.co///")
                .build();
        assertEquals("https://api.staging.rerout.co", rerout.getBaseUrl());
    }

    @Test
    void allThreeNamespacesArePresent() {
        Rerout rerout = Rerout.create("rrk_test");
        assertNotNull(rerout.links());
        assertNotNull(rerout.project());
        assertNotNull(rerout.qr());
    }

    @Test
    void aCustomHttpClientIsAccepted() {
        HttpClient custom = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        Rerout rerout = Rerout.builder("rrk_test").httpClient(custom).build();
        assertNotNull(rerout.links());
    }

    // ─── Request transport ──────────────────────────────────────────────────

    @Test
    void bearerAuthHeaderIsSentOnEveryCall() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client("rrk_secret").links().get("q4");

        RecordedRequest recorded = api.takeRequest();
        assertEquals("Bearer rrk_secret", recorded.header("Authorization"));
    }

    @Test
    void contentTypeIsSetOnlyWhenABodyIsSent() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().create(
                CreateLinkInput.builder("https://example.com").build());
        RecordedRequest withBody = api.takeRequest();
        assertEquals("application/json", withBody.header("Content-Type"));

        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().get("q4");
        RecordedRequest withoutBody = api.takeRequest();
        assertNull(withoutBody.header("Content-Type"));
    }

    @Test
    void jsonBodyIsSerializedWithSnakeCaseKeys() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().create(
                CreateLinkInput.builder("https://example.com/sale")
                        .domainHostname("go.brand.com")
                        .code("q4")
                        .seoTitle("Sale")
                        .build());
        String body = api.takeRequest().body;
        assertTrue(body.contains("\"target_url\":\"https://example.com/sale\""));
        assertTrue(body.contains("\"domain_hostname\":\"go.brand.com\""));
        assertTrue(body.contains("\"seo_title\":\"Sale\""));
    }

    @Test
    void optionalCreateFieldsAreOmittedWhenNull() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().create(
                CreateLinkInput.builder("https://example.com").build());
        String body = api.takeRequest().body;
        assertFalse(body.contains("domain_hostname"));
        assertFalse(body.contains("seo_title"));
    }

    @Test
    void queryParamsForCursorAndLimitAreEmitted() {
        api.enqueue(MockResponse.json("{\"links\":[],\"next_cursor\":null}"));
        api.client().links().list(40L, 10);
        assertEquals("/v1/links?cursor=40&limit=10", api.takeRequest().path);
    }

    @Test
    void queryParamsAreOmittedWhenListArgsAbsent() {
        api.enqueue(MockResponse.json("{\"links\":[],\"next_cursor\":null}"));
        api.client().links().list();
        assertEquals("/v1/links", api.takeRequest().path);
    }

    @Test
    void daysQueryParamIsEmittedForStats() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"q4\",\"days\":7,\"total_clicks\":0,\"qr_scans\":0}"));
        api.client().links().stats("q4", 7);
        assertEquals("/v1/links/q4/stats?days=7", api.takeRequest().path);
    }

    @Test
    void userAgentHeaderIdentifiesTheSdk() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().get("q4");
        String ua = api.takeRequest().header("User-Agent");
        assertNotNull(ua);
        assertTrue(ua.startsWith("rerout-java/"));
    }

    @Test
    void serverErrorCodeAndMessageArePreserved() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"bad_target_url\",\"message\":\"target_url must be https\"}",
                400));
        ReroutException ex = assertThrows(ReroutException.class, () ->
                api.client().links().create(
                        CreateLinkInput.builder("ftp://x").build()));
        assertEquals("bad_target_url", ex.getCode());
        assertEquals("target_url must be https", ex.getMessage());
        assertEquals(400, ex.getStatus());
    }

    @Test
    void serverErrorTimestampIsPreservedWhenPresent() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"rate_limited\",\"message\":\"slow down\","
                + "\"timestamp\":\"2026-05-22T10:00:00Z\"}",
                429));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().get("q4"));
        assertEquals("2026-05-22T10:00:00Z", ex.getTimestamp());
        assertTrue(ex.isRateLimited());
    }

    @ParameterizedTest
    @CsvSource({
        "401, unauthorized",
        "403, forbidden",
        "404, not_found",
        "429, rate_limited",
        "500, server_error",
        "503, server_error",
        "418, client_error",
    })
    void syntheticCodesAreUsedForEmptyErrorBodies(int status, String expectedCode) {
        api.enqueue(MockResponse.status(status));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().get("q4"));
        assertEquals(expectedCode, ex.getCode());
        assertEquals(status, ex.getStatus());
    }

    @Test
    void nonJsonErrorBodyFallsBackToASyntheticCode() {
        api.enqueue(MockResponse.text("<html>oops</html>", 502, "text/html"));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().get("q4"));
        assertEquals("server_error", ex.getCode());
        assertEquals(502, ex.getStatus());
    }

    @Test
    void isServerErrorFlagIsTrueFor5xx() {
        api.enqueue(MockResponse.status(500));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().get("q4"));
        assertTrue(ex.isServerError());
        assertFalse(ex.isRateLimited());
    }

    @Test
    void networkFailureSurfacesAsNetworkError() {
        try (MockApi.BrokenServer broken = new MockApi.BrokenServer()) {
            Rerout rerout = Rerout.builder("rrk_test")
                    .baseUrl(broken.baseUrl())
                    .build();
            ReroutException ex = assertThrows(
                    ReroutException.class, () -> rerout.links().get("q4"));
            assertEquals("network_error", ex.getCode());
            assertEquals(0, ex.getStatus());
        }
    }

    @Test
    void connectionRefusedSurfacesAsNetworkError() {
        MockApi dead = new MockApi();
        String baseUrl = dead.baseUrl();
        dead.close();
        Rerout rerout = Rerout.builder("rrk_test").baseUrl(baseUrl).build();
        ReroutException ex = assertThrows(
                ReroutException.class, () -> rerout.links().get("q4"));
        assertEquals("network_error", ex.getCode());
    }

    @Test
    void timeoutSurfacesAsTimeoutCode() {
        api.enqueue(MockResponse.delayed(MockApi.SAMPLE_LINK_JSON, 2_000));
        Rerout rerout = Rerout.builder("rrk_test")
                .baseUrl(api.baseUrl())
                .timeout(Duration.ofMillis(200))
                .build();
        ReroutException ex = assertThrows(
                ReroutException.class, () -> rerout.links().get("q4"));
        assertEquals("timeout", ex.getCode());
        assertEquals(0, ex.getStatus());
    }

    @Test
    void unexpectedResponseIsRaisedForA2xxNonJsonBody() {
        api.enqueue(MockResponse.text("not json at all", 200, "text/plain"));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().get("q4"));
        assertEquals("unexpected_response", ex.getCode());
    }

    @Test
    void unexpectedResponseIsRaisedForAnEmpty2xxBody() {
        api.enqueue(MockResponse.status(200));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().get("q4"));
        assertEquals("unexpected_response", ex.getCode());
    }

    // ─── async transport ────────────────────────────────────────────────────

    @Test
    void asyncFormCompletesWithTheParsedValue() throws Exception {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        assertEquals("q4", api.client().links().getAsync("q4").get().getCode());
    }

    @Test
    void asyncFormCompletesExceptionallyWithReroutException() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"not_found\",\"message\":\"no link\"}", 404));
        ExecutionException ex = assertThrows(
                ExecutionException.class,
                () -> api.client().links().getAsync("nope").get());
        assertTrue(ex.getCause() instanceof ReroutException);
        assertEquals("not_found", ((ReroutException) ex.getCause()).getCode());
    }
}
