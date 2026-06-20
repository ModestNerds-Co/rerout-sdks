/*
 * rerout-kotlin — tag management namespace tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TagsTest {
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    // ─── list ───────────────────────────────────────────────────────────────

    @Test
    fun `list GETs the tags endpoint and returns link counts`() = runTest {
        server.enqueue(jsonResponse("""{"tags":[$SAMPLE_TAG_SUMMARY_JSON]}"""))
        val result = server.client().tags.list()
        assertEquals(1, result.tags.size)
        assertEquals("tag_abc123", result.tags[0].id)
        assertEquals("Spring 2026", result.tags[0].name)
        assertEquals("teal", result.tags[0].color)
        assertEquals(4, result.tags[0].linkCount)
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/projects/me/tags", recorded.path)
    }

    @Test
    fun `list tolerates an empty tags array`() = runTest {
        server.enqueue(jsonResponse("""{"tags":[]}"""))
        val result = server.client().tags.list()
        assertTrue(result.tags.isEmpty())
    }

    @Test
    fun `list surfaces an auth error`() {
        server.enqueue(jsonResponse("""{"code":"unauthorized","message":"bad key"}""", status = 401))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().tags.list() }
        }
        assertEquals("unauthorized", ex.code)
        assertEquals(401, ex.status)
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Test
    fun `create posts the name and color`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_TAG_JSON, status = 201))
        val tag = server.client().tags.create(CreateTagInput(name = "Spring 2026", color = "teal"))
        assertEquals("tag_abc123", tag.id)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/projects/me/tags", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"name\":\"Spring 2026\""))
        assertTrue(body.contains("\"color\":\"teal\""))
    }

    @Test
    fun `create omits color when not provided`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_TAG_JSON, status = 201))
        server.client().tags.create(CreateTagInput(name = "Spring 2026"))
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"name\":\"Spring 2026\""))
        assertFalse(body.contains("color"))
    }

    @Test
    fun `create surfaces a validation error`() {
        server.enqueue(jsonResponse("""{"code":"bad_request","message":"bad color"}""", status = 422))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().tags.create(CreateTagInput(name = "x", color = "neon")) }
        }
        assertEquals("bad_request", ex.code)
        assertEquals(422, ex.status)
    }

    // ─── update ─────────────────────────────────────────────────────────────

    @Test
    fun `update PATCHes the tag by id and forwards only the color`() = runTest {
        server.enqueue(jsonResponse("""{"id":"tag_abc123","name":"Spring 2026","color":"red"}"""))
        val tag = server.client().tags.update("tag_abc123", UpdateTagInput(color = "red"))
        assertEquals("red", tag.color)
        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/v1/projects/me/tags/tag_abc123", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"color\":\"red\""))
        assertFalse(body.contains("name"))
    }

    @Test
    fun `update forwards only the provided name`() = runTest {
        server.enqueue(jsonResponse("""{"id":"tag_abc123","name":"Renamed","color":"teal"}"""))
        val tag = server.client().tags.update("tag_abc123", UpdateTagInput(name = "Renamed"))
        assertEquals("Renamed", tag.name)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"name\":\"Renamed\""))
        assertFalse(body.contains("color"))
    }

    @Test
    fun `update rejects an empty patch client-side without hitting the API`() {
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().tags.update("tag_abc123", UpdateTagInput()) }
        }
        assertEquals("bad_request", ex.code)
        assertEquals(0, ex.status)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `update surfaces a not-found error`() {
        server.enqueue(jsonResponse("""{"code":"not_found","message":"no tag"}""", status = 404))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().tags.update("tag_missing", UpdateTagInput(name = "x")) }
        }
        assertEquals("not_found", ex.code)
        assertEquals(404, ex.status)
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Test
    fun `delete sends DELETE to the tag id and returns the deleted flag`() = runTest {
        server.enqueue(jsonResponse("""{"deleted":true}"""))
        val result = server.client().tags.delete("tag_abc123")
        assertTrue(result.deleted)
        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/v1/projects/me/tags/tag_abc123", recorded.path)
    }

    @Test
    fun `delete tolerates an empty success body`() = runTest {
        server.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(204))
        val result = server.client().tags.delete("tag_abc123")
        assertTrue(result.deleted)
    }

    @Test
    fun `delete surfaces a forbidden error`() {
        server.enqueue(jsonResponse("""{"code":"forbidden","message":"not yours"}""", status = 403))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().tags.delete("tag_abc123") }
        }
        assertEquals("forbidden", ex.code)
    }

    // ─── URL encoding ───────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource(
        "tag_abc123, /v1/projects/me/tags/tag_abc123",
        "tag a/b, /v1/projects/me/tags/tag%20a%2Fb",
    )
    fun `tag ids are url-encoded as a single path segment`(id: String, expectedPath: String) {
        server.enqueue(jsonResponse("""{"deleted":true}"""))
        runTest { server.client().tags.delete(id) }
        assertEquals(expectedPath, server.takeRequest().path)
    }

    // ─── namespace ──────────────────────────────────────────────────────────

    @Test
    fun `tags namespace is present on the client`() {
        org.junit.jupiter.api.Assertions.assertNotNull(Rerout(apiKey = "rrk_test").tags)
    }
}
