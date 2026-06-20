/*
 * rerout-java — tag management namespace tests.
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
import co.rerout.sdk.model.CreateTagInput;
import co.rerout.sdk.model.DeleteResult;
import co.rerout.sdk.model.ListTagsResult;
import co.rerout.sdk.model.Tag;
import co.rerout.sdk.model.UpdateTagInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TagsTest {

    private MockApi api;

    @BeforeEach
    void setUp() {
        api = new MockApi();
    }

    @AfterEach
    void tearDown() {
        api.close();
    }

    // ─── list ───────────────────────────────────────────────────────────────

    @Test
    void listGetsTheTagsEndpointAndParsesLinkCounts() {
        api.enqueue(MockResponse.json(
                "{\"tags\":[" + MockApi.SAMPLE_TAG_SUMMARY_JSON + "]}"));
        ListTagsResult result = api.client().tags().list();
        assertEquals(1, result.getTags().size());
        assertEquals("tag_abc123", result.getTags().get(0).getId());
        assertEquals("Spring 2026", result.getTags().get(0).getName());
        assertEquals("teal", result.getTags().get(0).getColor());
        assertEquals(4, result.getTags().get(0).getLinkCount());
        RecordedRequest recorded = api.takeRequest();
        assertEquals("GET", recorded.method);
        assertEquals("/v1/projects/me/tags", recorded.path);
    }

    @Test
    void listToleratesAnEmptyTagList() {
        api.enqueue(MockResponse.json("{\"tags\":[]}"));
        ListTagsResult result = api.client().tags().list();
        assertTrue(result.getTags().isEmpty());
    }

    @Test
    void listSurfacesAnAuthError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"unauthorized\",\"message\":\"bad key\"}", 401));
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().tags().list());
        assertEquals("unauthorized", ex.getCode());
        assertEquals(401, ex.getStatus());
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Test
    void createPostsTheNameAndColor() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_TAG_JSON, 201));
        Tag tag = api.client().tags().create(
                CreateTagInput.builder("Spring 2026").color("teal").build());
        assertEquals("tag_abc123", tag.getId());
        RecordedRequest recorded = api.takeRequest();
        assertEquals("POST", recorded.method);
        assertEquals("/v1/projects/me/tags", recorded.path);
        assertTrue(recorded.body.contains("\"name\":\"Spring 2026\""));
        assertTrue(recorded.body.contains("\"color\":\"teal\""));
    }

    @Test
    void createOmitsColorWhenUnset() {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_TAG_JSON, 201));
        api.client().tags().create(CreateTagInput.builder("Spring 2026").build());
        String body = api.takeRequest().body;
        assertTrue(body.contains("\"name\":\"Spring 2026\""));
        assertFalse(body.contains("color"));
    }

    @Test
    void createSurfacesAServerError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"bad_request\",\"message\":\"bad color\"}", 422));
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().tags().create(
                        CreateTagInput.builder("Spring 2026").color("not-a-color").build()));
        assertEquals("bad_request", ex.getCode());
        assertEquals(422, ex.getStatus());
    }

    @Test
    void createAsyncCompletesWithTheParsedResult() throws Exception {
        api.enqueue(MockResponse.json(MockApi.SAMPLE_TAG_JSON, 201));
        Tag tag = api.client().tags()
                .createAsync(CreateTagInput.builder("Spring 2026").build()).get();
        assertEquals("tag_abc123", tag.getId());
    }

    // ─── update ─────────────────────────────────────────────────────────────

    @Test
    void updatePatchesTheTagByIdAndEncodesIt() {
        api.enqueue(MockResponse.json(
                "{\"id\":\"tag_abc123\",\"name\":\"Spring 2026\",\"color\":\"red\"}"));
        Tag tag = api.client().tags().update("tag_abc123",
                UpdateTagInput.builder().color("red").build());
        assertEquals("red", tag.getColor());
        RecordedRequest recorded = api.takeRequest();
        assertEquals("PATCH", recorded.method);
        assertEquals("/v1/projects/me/tags/tag_abc123", recorded.path);
        assertEquals("{\"color\":\"red\"}", recorded.body);
    }

    @Test
    void updateForwardsOnlyTheProvidedFields() {
        api.enqueue(MockResponse.json(
                "{\"id\":\"tag_abc123\",\"name\":\"Renamed\",\"color\":\"teal\"}"));
        Tag tag = api.client().tags().update("tag_abc123",
                UpdateTagInput.builder().name("Renamed").build());
        assertEquals("Renamed", tag.getName());
        assertEquals("{\"name\":\"Renamed\"}", api.takeRequest().body);
    }

    @Test
    void updateRejectsAnEmptyPatchClientSide() {
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().tags().update("tag_abc123",
                        UpdateTagInput.builder().build()));
        assertEquals("bad_request", ex.getCode());
        assertEquals(0, api.requestCount());
    }

    @Test
    void updateSurfacesAServerError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"not_found\",\"message\":\"no such tag\"}", 404));
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().tags().update("tag_missing",
                        UpdateTagInput.builder().name("x").build()));
        assertEquals("not_found", ex.getCode());
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Test
    void deleteSendsDeleteToTheTagPathAndReturnsTheDeletedFlag() {
        api.enqueue(MockResponse.json("{\"deleted\":true}"));
        DeleteResult result = api.client().tags().delete("tag_abc123");
        assertTrue(result.isDeleted());
        RecordedRequest recorded = api.takeRequest();
        assertEquals("DELETE", recorded.method);
        assertEquals("/v1/projects/me/tags/tag_abc123", recorded.path);
    }

    @Test
    void deleteToleratesAnEmptySuccessBody() {
        api.enqueue(MockResponse.status(204));
        assertTrue(api.client().tags().delete("tag_abc123").isDeleted());
    }

    @Test
    void deleteSurfacesAForbiddenError() {
        api.enqueue(MockResponse.json(
                "{\"code\":\"forbidden\",\"message\":\"not yours\"}", 403));
        ReroutException ex = assertThrows(ReroutException.class,
                () -> api.client().tags().delete("tag_abc123"));
        assertEquals("forbidden", ex.getCode());
    }

    // ─── URL encoding edge cases ────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "tag_abc123, /v1/projects/me/tags/tag_abc123",
        "tag a/b, /v1/projects/me/tags/tag%20a%2Fb",
        "tag+x, /v1/projects/me/tags/tag%2Bx",
    })
    void tagIdsAreUrlEncodedAsASinglePathSegment(String id, String expectedPath) {
        api.enqueue(MockResponse.json("{\"deleted\":true}"));
        api.client().tags().delete(id);
        assertEquals(expectedPath, api.takeRequest().path);
    }
}
