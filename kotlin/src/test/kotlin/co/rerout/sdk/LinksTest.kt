/*
 * rerout-kotlin — links and project namespace tests.
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

class LinksTest {
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

    // ─── create ─────────────────────────────────────────────────────────────

    @Test
    fun `create returns the parsed link`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        val link = server.client().links.create(
            CreateLinkInput(targetUrl = "https://example.com/q4-sale", code = "q4"),
        )
        assertEquals("q4", link.code)
        assertEquals("https://go.brand.com/q4", link.shortUrl)
        assertEquals("go.brand.com", link.domainHostname)
        assertEquals("prj_123", link.projectId)
        assertTrue(link.isActive)
        assertFalse(link.seoNoindex)
    }

    @Test
    fun `create posts to the links endpoint`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.create(CreateLinkInput(targetUrl = "https://example.com"))
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/links", recorded.path)
    }

    @Test
    fun `create surfaces a server error`() {
        server.enqueue(jsonResponse("""{"code":"bad_target_url","message":"bad url"}""", status = 422))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.create(CreateLinkInput(targetUrl = "x")) }
        }
        assertEquals("bad_target_url", ex.code)
        assertEquals(422, ex.status)
    }

    // ─── list ───────────────────────────────────────────────────────────────

    @Test
    fun `list parses links and cursor`() = runTest {
        server.enqueue(
            jsonResponse("""{"links":[$SAMPLE_LINK_JSON],"next_cursor":80}"""),
        )
        val result = server.client().links.list()
        assertEquals(1, result.links.size)
        assertEquals("q4", result.links[0].code)
        assertEquals(80L, result.nextCursor)
        assertTrue(result.hasMore)
    }

    @Test
    fun `list reports no more pages when cursor is null`() = runTest {
        server.enqueue(jsonResponse("""{"links":[],"next_cursor":null}"""))
        val result = server.client().links.list()
        assertTrue(result.links.isEmpty())
        assertFalse(result.hasMore)
    }

    @Test
    fun `list surfaces an auth error`() {
        server.enqueue(jsonResponse("""{"code":"unauthorized","message":"bad key"}""", status = 401))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.list() }
        }
        assertEquals("unauthorized", ex.code)
    }

    // ─── get ────────────────────────────────────────────────────────────────

    @Test
    fun `get fetches a single link`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        val link = server.client().links.get("q4")
        assertEquals("q4", link.code)
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun `get surfaces a not found error`() {
        server.enqueue(jsonResponse("""{"code":"not_found","message":"no such link"}""", status = 404))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.get("nope") }
        }
        assertEquals("not_found", ex.code)
        assertEquals(404, ex.status)
    }

    // ─── update ─────────────────────────────────────────────────────────────

    @Test
    fun `update patches only the fields that are set`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.update(
            "q4",
            UpdateLinkInput.builder().targetUrl("https://example.com/new").build(),
        )
        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"target_url\":\"https://example.com/new\""))
        assertFalse(body.contains("seo_title"))
        assertFalse(body.contains("expires_at"))
    }

    @Test
    fun `update sends explicit null for a cleared field`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.update(
            "q4",
            UpdateLinkInput.builder().clearExpiresAt().clearSeoTitle().build(),
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"expires_at\":null"))
        assertTrue(body.contains("\"seo_title\":null"))
    }

    @Test
    fun `update with a boolean toggle is serialized`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.update("q4", UpdateLinkInput.builder().isActive(false).build())
        assertTrue(server.takeRequest().body.readUtf8().contains("\"is_active\":false"))
    }

    @Test
    fun `update rejects an empty patch without calling the api`() {
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.update("q4", UpdateLinkInput.builder().build()) }
        }
        assertEquals("bad_request", ex.code)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `update keeps a real expiry over the clear flag when only the value is set`() {
        val input = UpdateLinkInput.builder().expiresAt(1717000000).build()
        assertEquals(1717000000L, input.toJsonMap()["expires_at"])
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Test
    fun `delete returns the deleted flag`() = runTest {
        server.enqueue(jsonResponse("""{"deleted":true}"""))
        val result = server.client().links.delete("q4")
        assertTrue(result.deleted)
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `delete tolerates an empty success body`() = runTest {
        server.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(204))
        val result = server.client().links.delete("q4")
        assertTrue(result.deleted)
    }

    @Test
    fun `delete surfaces a forbidden error`() {
        server.enqueue(jsonResponse("""{"code":"forbidden","message":"not yours"}""", status = 403))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.delete("q4") }
        }
        assertEquals("forbidden", ex.code)
    }

    // ─── stats ──────────────────────────────────────────────────────────────

    @Test
    fun `link stats parses totals and breakdowns`() = runTest {
        server.enqueue(
            jsonResponse(
                """
                {
                  "code": "q4",
                  "days": 30,
                  "total_clicks": 1280,
                  "qr_scans": 210,
                  "countries": [{"value":"ZA","clicks":900},{"value":"US","clicks":380}],
                  "referrers": [{"value":"twitter.com","clicks":500}]
                }
                """,
            ),
        )
        val stats = server.client().links.stats("q4")
        assertEquals("q4", stats.code)
        assertEquals(1280L, stats.totalClicks)
        assertEquals(210L, stats.qrScans)
        assertEquals(2, stats.countries.size)
        assertEquals("ZA", stats.countries[0].value)
        assertEquals(900L, stats.countries[0].clicks)
    }

    @Test
    fun `link stats defaults to 30 days`() = runTest {
        server.enqueue(jsonResponse("""{"code":"q4","days":30,"total_clicks":0,"qr_scans":0}"""))
        server.client().links.stats("q4")
        assertEquals("/v1/links/q4/stats?days=30", server.takeRequest().path)
    }

    @Test
    fun `link stats surfaces a server error`() {
        server.enqueue(jsonResponse("""{"code":"server_error","message":"db down"}""", status = 500))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.stats("q4") }
        }
        assertEquals("server_error", ex.code)
        assertTrue(ex.isServerError)
    }

    // ─── project ────────────────────────────────────────────────────────────

    @Test
    fun `project stats parses the aggregate envelope`() = runTest {
        server.enqueue(
            jsonResponse(
                """
                {
                  "days": 30,
                  "total_clicks": 5000,
                  "qr_scans": 800,
                  "daily": [{"day":1716000000,"clicks":120,"qr_scans":30}],
                  "countries": [{"value":"ZA","clicks":3000}],
                  "referrers": [],
                  "devices": [{"value":"mobile","clicks":4000}],
                  "browsers": [],
                  "top_codes": [{"value":"q4","clicks":2000}]
                }
                """,
            ),
        )
        val stats = server.client().project.stats()
        assertEquals(5000L, stats.totalClicks)
        assertEquals(1, stats.daily.size)
        assertEquals(120L, stats.daily[0].clicks)
        assertEquals("mobile", stats.devices[0].value)
        assertEquals("q4", stats.topCodes[0].value)
    }

    @Test
    fun `project stats honours a custom day window`() = runTest {
        server.enqueue(jsonResponse("""{"days":7,"total_clicks":0,"qr_scans":0}"""))
        server.client().project.stats(days = 7)
        assertEquals("/v1/projects/me/stats?days=7", server.takeRequest().path)
    }

    @Test
    fun `project me returns the current project`() = runTest {
        server.enqueue(jsonResponse("""{"id":"prj_123","name":"Acme","slug":"acme"}"""))
        val project = server.client().project.me()
        assertEquals("prj_123", project.id)
        assertEquals("Acme", project.name)
        assertEquals("acme", project.slug)
        assertEquals("/v1/projects/me", server.takeRequest().path)
    }

    @Test
    fun `project me surfaces an auth error`() {
        server.enqueue(jsonResponse("""{"code":"unauthorized","message":"bad key"}""", status = 401))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().project.me() }
        }
        assertEquals("unauthorized", ex.code)
    }

    // ─── URL encoding edge cases ────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource(
        "hello world, /v1/links/hello%20world",
        "a+b, /v1/links/a%2Bb",
        "café, /v1/links/caf%C3%A9",
        "go/promo, /v1/links/go%2Fpromo",
    )
    fun `link codes are url-encoded as a single path segment`(code: String, expectedPath: String) {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        runTest { server.client().links.get(code) }
        assertEquals(expectedPath, server.takeRequest().path)
    }
}
