/*
 * rerout-java — links and project namespace tests.
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
import co.rerout.sdk.model.CreateLinkInput;
import co.rerout.sdk.model.DeleteResult;
import co.rerout.sdk.model.Link;
import co.rerout.sdk.model.LinkStats;
import co.rerout.sdk.model.ListLinksResult;
import co.rerout.sdk.model.ProjectInfo;
import co.rerout.sdk.model.ProjectStats;
import co.rerout.sdk.model.UpdateLinkInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LinksTest {

    private MockApi api;

    @BeforeEach
    void setUp() {
        api = new MockApi();
    }

    @AfterEach
    void tearDown() {
        api.close();
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Test
    void createReturnsTheParsedLink() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        Link link = api.client().links().create(
                CreateLinkInput.builder("https://example.com/q4-sale").code("q4").build());
        assertEquals("q4", link.getCode());
        assertEquals("https://go.brand.com/q4", link.getShortUrl());
        assertEquals("go.brand.com", link.getDomainHostname());
        assertEquals("prj_123", link.getProjectId());
        assertTrue(link.isActive());
        assertFalse(link.isSeoNoindex());
    }

    @Test
    void createPostsToTheLinksEndpoint() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().create(
                CreateLinkInput.builder("https://example.com").build());
        RecordedRequest recorded = api.takeRequest();
        assertEquals("POST", recorded.method);
        assertEquals("/v1/links", recorded.path);
    }

    @Test
    void createSurfacesAServerError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"bad_target_url\",\"message\":\"bad url\"}", 422));
        ReroutException ex = assertThrows(ReroutException.class, () ->
                api.client().links().create(
                        CreateLinkInput.builder("x").build()));
        assertEquals("bad_target_url", ex.getCode());
        assertEquals(422, ex.getStatus());
    }

    @Test
    void createAsyncCompletesWithTheParsedLink() throws Exception {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        Link link = api.client().links()
                .createAsync(CreateLinkInput.builder("https://example.com").build())
                .get();
        assertEquals("q4", link.getCode());
    }

    // ─── list ───────────────────────────────────────────────────────────────

    @Test
    void listParsesLinksAndCursor() {
        api.enqueue(MockResponse.json(
                "{\"links\":[" + MockApi.SAMPLE_LINK_JSON + "],\"next_cursor\":80}"));
        ListLinksResult result = api.client().links().list();
        assertEquals(1, result.getLinks().size());
        assertEquals("q4", result.getLinks().get(0).getCode());
        assertEquals(Long.valueOf(80), result.getNextCursor());
        assertTrue(result.hasMore());
    }

    @Test
    void listReportsNoMorePagesWhenCursorIsNull() {
        api.enqueue(MockResponse.json("{\"links\":[],\"next_cursor\":null}"));
        ListLinksResult result = api.client().links().list();
        assertTrue(result.getLinks().isEmpty());
        assertFalse(result.hasMore());
    }

    @Test
    void listSurfacesAnAuthError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"unauthorized\",\"message\":\"bad key\"}", 401));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().list());
        assertEquals("unauthorized", ex.getCode());
    }

    // ─── get ────────────────────────────────────────────────────────────────

    @Test
    void getFetchesASingleLink() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        Link link = api.client().links().get("q4");
        assertEquals("q4", link.getCode());
        assertEquals("GET", api.takeRequest().method);
    }

    @Test
    void getSurfacesANotFoundError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"not_found\",\"message\":\"no such link\"}", 404));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().get("nope"));
        assertEquals("not_found", ex.getCode());
        assertEquals(404, ex.getStatus());
    }

    // ─── update ─────────────────────────────────────────────────────────────

    @Test
    void updatePatchesOnlyTheFieldsThatAreSet() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().update("q4",
                UpdateLinkInput.builder().targetUrl("https://example.com/new").build());
        RecordedRequest recorded = api.takeRequest();
        assertEquals("PATCH", recorded.method);
        assertTrue(recorded.body.contains("\"target_url\":\"https://example.com/new\""));
        assertFalse(recorded.body.contains("seo_title"));
        assertFalse(recorded.body.contains("expires_at"));
    }

    @Test
    void updateSendsExplicitNullForAClearedField() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().update("q4",
                UpdateLinkInput.builder().clearExpiresAt().clearSeoTitle().build());
        String body = api.takeRequest().body;
        assertTrue(body.contains("\"expires_at\":null"));
        assertTrue(body.contains("\"seo_title\":null"));
    }

    @Test
    void updateWithABooleanToggleIsSerialized() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().update("q4",
                UpdateLinkInput.builder().active(false).build());
        assertTrue(api.takeRequest().body.contains("\"is_active\":false"));
    }

    @Test
    void updateRejectsAnEmptyPatchWithoutCallingTheApi() {
        ReroutException ex = assertThrows(ReroutException.class, () ->
                api.client().links().update("q4", UpdateLinkInput.builder().build()));
        assertEquals("bad_request", ex.getCode());
        assertEquals(0, api.requestCount());
    }

    @Test
    void updateKeepsARealExpiryWhenOnlyTheValueIsSet() {
        UpdateLinkInput input = UpdateLinkInput.builder().expiresAt(1717000000L).build();
        assertEquals(1717000000L, input.toJsonMap().get("expires_at"));
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Test
    void deleteReturnsTheDeletedFlag() {
        api.enqueue(MockResponse.json("{\"deleted\":true}"));
        DeleteResult result = api.client().links().delete("q4");
        assertTrue(result.isDeleted());
        assertEquals("DELETE", api.takeRequest().method);
    }

    @Test
    void deleteToleratesAnEmptySuccessBody() {
        api.enqueue(MockResponse.status(204));
        assertTrue(api.client().links().delete("q4").isDeleted());
    }

    @Test
    void deleteSurfacesAForbiddenError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"forbidden\",\"message\":\"not yours\"}", 403));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().delete("q4"));
        assertEquals("forbidden", ex.getCode());
    }

    // ─── stats ──────────────────────────────────────────────────────────────

    @Test
    void linkStatsParsesTotalsAndBreakdowns() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"q4\",\"days\":30,\"total_clicks\":1280,\"qr_scans\":210,"
                + "\"countries\":[{\"value\":\"ZA\",\"clicks\":900},"
                + "{\"value\":\"US\",\"clicks\":380}],"
                + "\"referrers\":[{\"value\":\"twitter.com\",\"clicks\":500}]}"));
        LinkStats stats = api.client().links().stats("q4");
        assertEquals("q4", stats.getCode());
        assertEquals(1280L, stats.getTotalClicks());
        assertEquals(210L, stats.getQrScans());
        assertEquals(2, stats.getCountries().size());
        assertEquals("ZA", stats.getCountries().get(0).getValue());
        assertEquals(900L, stats.getCountries().get(0).getClicks());
    }

    @Test
    void linkStatsDefaultsTo30Days() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"q4\",\"days\":30,\"total_clicks\":0,\"qr_scans\":0}"));
        api.client().links().stats("q4");
        assertEquals("/v1/links/q4/stats?days=30", api.takeRequest().path);
    }

    @Test
    void linkStatsSurfacesAServerError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"server_error\",\"message\":\"db down\"}", 500));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().links().stats("q4"));
        assertEquals("server_error", ex.getCode());
        assertTrue(ex.isServerError());
    }

    // ─── project ────────────────────────────────────────────────────────────

    @Test
    void projectStatsParsesTheAggregateEnvelope() {
        api.enqueue(MockResponse.json(
                "{\"days\":30,\"total_clicks\":5000,\"qr_scans\":800,"
                + "\"daily\":[{\"day\":1716000000,\"clicks\":120,\"qr_scans\":30}],"
                + "\"countries\":[{\"value\":\"ZA\",\"clicks\":3000}],"
                + "\"referrers\":[],"
                + "\"devices\":[{\"value\":\"mobile\",\"clicks\":4000}],"
                + "\"browsers\":[],"
                + "\"top_codes\":[{\"value\":\"q4\",\"clicks\":2000}]}"));
        ProjectStats stats = api.client().project().stats();
        assertEquals(5000L, stats.getTotalClicks());
        assertEquals(1, stats.getDaily().size());
        assertEquals(120L, stats.getDaily().get(0).getClicks());
        assertEquals("mobile", stats.getDevices().get(0).getValue());
        assertEquals("q4", stats.getTopCodes().get(0).getValue());
    }

    @Test
    void projectStatsHonoursACustomDayWindow() {
        api.enqueue(MockResponse.json(
                "{\"days\":7,\"total_clicks\":0,\"qr_scans\":0}"));
        api.client().project().stats(7);
        assertEquals("/v1/projects/me/stats?days=7", api.takeRequest().path);
    }

    @Test
    void projectMeReturnsTheCurrentProject() {
        api.enqueue(MockResponse.json(
                "{\"id\":\"prj_123\",\"name\":\"Acme\",\"slug\":\"acme\"}"));
        ProjectInfo project = api.client().project().me();
        assertEquals("prj_123", project.getId());
        assertEquals("Acme", project.getName());
        assertEquals("acme", project.getSlug());
        assertEquals("/v1/projects/me", api.takeRequest().path);
    }

    @Test
    void projectMeSurfacesAnAuthError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"unauthorized\",\"message\":\"bad key\"}", 401));
        ReroutException ex = assertThrows(
                ReroutException.class, () -> api.client().project().me());
        assertEquals("unauthorized", ex.getCode());
    }

    @Test
    void projectStatsAsyncCompletesWithTheEnvelope() throws Exception {
        api.enqueue(MockResponse.json(
                "{\"days\":30,\"total_clicks\":42,\"qr_scans\":0}"));
        assertEquals(42L, api.client().project().statsAsync().get().getTotalClicks());
    }

    // ─── URL encoding edge cases ────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "hello world, /v1/links/hello%20world",
        "a+b, /v1/links/a%2Bb",
        "café, /v1/links/caf%C3%A9",
        "go/promo, /v1/links/go%2Fpromo",
    })
    void linkCodesAreUrlEncodedAsASinglePathSegment(String code, String expectedPath) {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_LINK_JSON));
        api.client().links().get(code);
        assertEquals(expectedPath, api.takeRequest().path);
    }
}
