/*
 * rerout-kotlin — client construction and transport tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.util.concurrent.TimeUnit

class ReroutTest {
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

    // ─── Constructor ────────────────────────────────────────────────────────

    @Test
    fun `constructor rejects blank api key`() {
        val ex = assertThrows(ReroutException::class.java) {
            Rerout(apiKey = "")
        }
        assertEquals("missing_api_key", ex.code)
        assertEquals(0, ex.status)
    }

    @Test
    fun `constructor rejects whitespace-only api key`() {
        val ex = assertThrows(ReroutException::class.java) {
            Rerout(apiKey = "   ")
        }
        assertEquals("missing_api_key", ex.code)
    }

    @Test
    fun `constructor accepts a valid api key`() {
        val rerout = Rerout(apiKey = "rrk_live_123")
        assertEquals(DEFAULT_BASE_URL, rerout.baseUrl)
    }

    @Test
    fun `base url trailing slashes are trimmed`() {
        val rerout = Rerout(apiKey = "rrk_test", baseUrl = "https://api.staging.rerout.co///")
        assertEquals("https://api.staging.rerout.co", rerout.baseUrl)
    }

    @Test
    fun `default base url is production`() {
        val rerout = Rerout(apiKey = "rrk_test")
        assertEquals("https://api.rerout.co", rerout.baseUrl)
    }

    @Test
    fun `all three namespaces are present`() {
        val rerout = Rerout(apiKey = "rrk_test")
        assertNotNull(rerout.links)
        assertNotNull(rerout.project)
        assertNotNull(rerout.qr)
    }

    @Test
    fun `a custom okhttp client is accepted`() {
        val custom = OkHttpClient.Builder().callTimeout(Duration.ofSeconds(5)).build()
        val rerout = Rerout(apiKey = "rrk_test", httpClient = custom)
        assertNotNull(rerout.links)
    }

    // ─── Request transport ──────────────────────────────────────────────────

    @Test
    fun `bearer auth header is sent on every call`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client("rrk_secret").links.get("q4")

        val recorded = server.takeRequest()
        assertEquals("Bearer rrk_secret", recorded.getHeader("Authorization"))
    }

    @Test
    fun `content-type is set only when a body is sent`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.create(CreateLinkInput(targetUrl = "https://example.com"))
        val withBody = server.takeRequest()
        assertEquals("application/json", withBody.getHeader("Content-Type"))

        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.get("q4")
        val withoutBody = server.takeRequest()
        assertNull(withoutBody.getHeader("Content-Type"))
    }

    @Test
    fun `json body is serialized with snake_case keys`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.create(
            CreateLinkInput(
                targetUrl = "https://example.com/sale",
                domainHostname = "go.brand.com",
                code = "q4",
                seoTitle = "Sale",
            ),
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"target_url\":\"https://example.com/sale\""))
        assertTrue(body.contains("\"domain_hostname\":\"go.brand.com\""))
        assertTrue(body.contains("\"seo_title\":\"Sale\""))
    }

    @Test
    fun `optional create fields are omitted when null`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.create(CreateLinkInput(targetUrl = "https://example.com"))
        val body = server.takeRequest().body.readUtf8()
        assertFalse(body.contains("domain_hostname"))
        assertFalse(body.contains("seo_title"))
    }

    @Test
    fun `query params for cursor and limit are emitted`() = runTest {
        server.enqueue(jsonResponse("""{"links":[],"next_cursor":null}"""))
        server.client().links.list(cursor = 40, limit = 10)
        val recorded = server.takeRequest()
        assertEquals("/v1/links?cursor=40&limit=10", recorded.path)
    }

    @Test
    fun `query params are omitted when list args absent`() = runTest {
        server.enqueue(jsonResponse("""{"links":[],"next_cursor":null}"""))
        server.client().links.list()
        assertEquals("/v1/links", server.takeRequest().path)
    }

    @Test
    fun `days query param is emitted for stats`() = runTest {
        server.enqueue(jsonResponse("""{"code":"q4","days":7,"total_clicks":0,"qr_scans":0}"""))
        server.client().links.stats("q4", days = 7)
        assertEquals("/v1/links/q4/stats?days=7", server.takeRequest().path)
    }

    @Test
    fun `user-agent header identifies the sdk`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.get("q4")
        val ua = server.takeRequest().getHeader("User-Agent")
        assertTrue(ua != null && ua.startsWith("rerout-kotlin/"))
    }

    @Test
    fun `server error code and message are preserved`() {
        server.enqueue(
            jsonResponse(
                """{"code":"bad_target_url","message":"target_url must be https"}""",
                status = 400,
            ),
        )
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.create(CreateLinkInput(targetUrl = "ftp://x")) }
        }
        assertEquals("bad_target_url", ex.code)
        assertEquals("target_url must be https", ex.message)
        assertEquals(400, ex.status)
    }

    @Test
    fun `server error timestamp is preserved when present`() {
        server.enqueue(
            jsonResponse(
                """{"code":"rate_limited","message":"slow down","timestamp":"2026-05-22T10:00:00Z"}""",
                status = 429,
            ),
        )
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.get("q4") }
        }
        assertEquals("2026-05-22T10:00:00Z", ex.timestamp)
        assertTrue(ex.isRateLimited)
    }

    @ParameterizedTest
    @CsvSource(
        "401, unauthorized",
        "403, forbidden",
        "404, not_found",
        "429, rate_limited",
        "500, server_error",
        "503, server_error",
        "418, client_error",
    )
    fun `synthetic codes are used for empty error bodies`(status: Int, expectedCode: String) {
        server.enqueue(MockResponse().setResponseCode(status))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.get("q4") }
        }
        assertEquals(expectedCode, ex.code)
        assertEquals(status, ex.status)
    }

    @Test
    fun `non-json error body falls back to a synthetic code`() {
        server.enqueue(textResponse("<html>oops</html>", status = 502, contentType = "text/html"))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.get("q4") }
        }
        assertEquals("server_error", ex.code)
        assertEquals(502, ex.status)
    }

    @Test
    fun `is server error flag is true for 5xx`() {
        server.enqueue(MockResponse().setResponseCode(500))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.get("q4") }
        }
        assertTrue(ex.isServerError)
        assertFalse(ex.isRateLimited)
    }

    @Test
    fun `network failure surfaces as network_error`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.get("q4") }
        }
        assertEquals("network_error", ex.code)
        assertEquals(0, ex.status)
    }

    @Test
    fun `connection refused surfaces as network_error`() {
        val deadServer = MockWebServer()
        deadServer.start()
        val baseUrl = deadServer.url("/").toString().trimEnd('/')
        deadServer.shutdown()
        val rerout = Rerout(apiKey = "rrk_test", baseUrl = baseUrl)
        val ex = assertThrows(ReroutException::class.java) {
            runTest { rerout.links.get("q4") }
        }
        assertEquals("network_error", ex.code)
    }

    @Test
    fun `timeout surfaces as timeout code`() {
        server.enqueue(
            MockResponse()
                .setBody(SAMPLE_LINK_JSON.trimIndent())
                .setBodyDelay(2, TimeUnit.SECONDS),
        )
        val fastClient = OkHttpClient.Builder()
            .callTimeout(Duration.ofMillis(200))
            .readTimeout(Duration.ofMillis(200))
            .build()
        val rerout = Rerout(
            apiKey = "rrk_test",
            baseUrl = server.url("/").toString().trimEnd('/'),
            httpClient = fastClient,
        )
        val ex = assertThrows(ReroutException::class.java) {
            runTest { rerout.links.get("q4") }
        }
        assertEquals("timeout", ex.code)
        assertEquals(0, ex.status)
    }

    @Test
    fun `unexpected_response is raised for a 2xx non-json body`() {
        server.enqueue(textResponse("not json at all", contentType = "text/plain"))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.get("q4") }
        }
        assertEquals("unexpected_response", ex.code)
    }

    @Test
    fun `unexpected_response is raised for an empty 2xx body`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().links.get("q4") }
        }
        assertEquals("unexpected_response", ex.code)
    }
}
