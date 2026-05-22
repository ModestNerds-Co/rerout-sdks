/*
 * rerout-kotlin — QR helper tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QrTest {
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

    // ─── url builder ────────────────────────────────────────────────────────

    @Test
    fun `bare url has no query string`() {
        val rerout = Rerout(apiKey = "rrk_test")
        assertEquals("https://api.rerout.co/v1/links/q4/qr", rerout.qr.url("q4"))
    }

    @Test
    fun `url honours a custom base url`() {
        val rerout = Rerout(apiKey = "rrk_test", baseUrl = "https://api.staging.rerout.co")
        assertEquals("https://api.staging.rerout.co/v1/links/q4/qr", rerout.qr.url("q4"))
    }

    @Test
    fun `url emits every option`() {
        val rerout = Rerout(apiKey = "rrk_test")
        val url = rerout.qr.url(
            "q4",
            QrOptions(size = 12, margin = 2, ecc = "H", domain = "go.brand.com"),
        )
        assertTrue(url.startsWith("https://api.rerout.co/v1/links/q4/qr?"))
        assertTrue(url.contains("size=12"))
        assertTrue(url.contains("margin=2"))
        assertTrue(url.contains("ecc=H"))
        assertTrue(url.contains("domain=go.brand.com"))
    }

    @Test
    fun `url with refresh enabled emits 1`() {
        val rerout = Rerout(apiKey = "rrk_test")
        val url = rerout.qr.url("q4", QrOptions(refresh = QrOptions.Refresh.Enabled))
        assertTrue(url.contains("refresh=1"))
    }

    @Test
    fun `url with a refresh token forwards it verbatim`() {
        val rerout = Rerout(apiKey = "rrk_test")
        val url = rerout.qr.url("q4", QrOptions(refresh = QrOptions.Refresh.token("v2")))
        assertTrue(url.contains("refresh=v2"))
    }

    @Test
    fun `url encodes a code with special characters`() {
        val rerout = Rerout(apiKey = "rrk_test")
        assertEquals(
            "https://api.rerout.co/v1/links/go%2Fpromo/qr",
            rerout.qr.url("go/promo"),
        )
    }

    @Test
    fun `url encodes a code with a space`() {
        val rerout = Rerout(apiKey = "rrk_test")
        assertEquals(
            "https://api.rerout.co/v1/links/hello%20world/qr",
            rerout.qr.url("hello world"),
        )
    }

    @Test
    fun `url with only some options omits the rest`() {
        val rerout = Rerout(apiKey = "rrk_test")
        val url = rerout.qr.url("q4", QrOptions(size = 8))
        assertEquals("https://api.rerout.co/v1/links/q4/qr?size=8", url)
    }

    // ─── svg fetch ──────────────────────────────────────────────────────────

    @Test
    fun `svg returns the rendered body`() = runTest {
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"
        server.enqueue(textResponse(svg))
        val result = server.client().qr.svg("q4")
        assertEquals(svg, result)
    }

    @Test
    fun `svg sends the bearer token`() = runTest {
        server.enqueue(textResponse("<svg></svg>"))
        server.client("rrk_secret").qr.svg("q4")
        assertEquals("Bearer rrk_secret", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `svg forwards options as query params`() = runTest {
        server.enqueue(textResponse("<svg></svg>"))
        server.client().qr.svg("q4", QrOptions(size = 16, ecc = "Q"))
        val path = server.takeRequest().path
        assertTrue(path!!.startsWith("/v1/links/q4/qr?"))
        assertTrue(path.contains("size=16"))
        assertTrue(path.contains("ecc=Q"))
    }

    @Test
    fun `svg surfaces a not found error`() {
        server.enqueue(jsonResponse("""{"code":"not_found","message":"no link"}""", status = 404))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().qr.svg("nope") }
        }
        assertEquals("not_found", ex.code)
    }

    @Test
    fun `svg raises unexpected_response on an empty body`() {
        server.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(200))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().qr.svg("q4") }
        }
        assertEquals("unexpected_response", ex.code)
    }
}
