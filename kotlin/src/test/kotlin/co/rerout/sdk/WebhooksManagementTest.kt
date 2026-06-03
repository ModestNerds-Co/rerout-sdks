/*
 * rerout-kotlin — webhook endpoint management namespace tests.
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class WebhooksManagementTest {
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
    fun `create posts to the webhooks endpoint with the right body`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"endpoint":$SAMPLE_WEBHOOK_JSON,"signing_secret":"whsec_supersecret"}""",
                status = 201,
            ),
        )
        server.client().webhooks.create(
            CreateWebhookInput(
                name = "Order events",
                url = "https://example.com/hooks/rerout",
                events = listOf("link.created", "link.clicked"),
            ),
        )
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/projects/me/webhooks", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"name\":\"Order events\""))
        assertTrue(body.contains("\"url\":\"https://example.com/hooks/rerout\""))
        assertTrue(body.contains("\"events\":[\"link.created\",\"link.clicked\"]"))
        // Optional fields are omitted when not set.
        assertFalse(body.contains("is_active"))
        assertFalse(body.contains("payload_format"))
    }

    @Test
    fun `create returns the signing secret and parsed endpoint`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"endpoint":$SAMPLE_WEBHOOK_JSON,"signing_secret":"whsec_supersecret"}""",
                status = 201,
            ),
        )
        val result = server.client().webhooks.create(
            CreateWebhookInput(
                name = "Order events",
                url = "https://example.com/hooks/rerout",
                events = listOf("link.created", "link.clicked"),
            ),
        )
        assertEquals("whsec_supersecret", result.signingSecret)
        assertEquals("wh_abc123", result.endpoint.id)
        assertEquals("prj_test", result.endpoint.projectId)
        assertEquals("Order events", result.endpoint.name)
        assertEquals("https://example.com/hooks/rerout", result.endpoint.url)
        assertEquals(listOf("link.created", "link.clicked"), result.endpoint.events)
        assertTrue(result.endpoint.isActive)
        assertEquals("json", result.endpoint.payloadFormat)
        assertNull(result.endpoint.lastDeliveryAt)
    }

    @Test
    fun `create forwards is_active and payload_format when provided`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"endpoint":$SAMPLE_WEBHOOK_JSON,"signing_secret":"whsec_x"}""",
                status = 201,
            ),
        )
        server.client().webhooks.create(
            CreateWebhookInput(
                name = "Slack",
                url = "https://hooks.slack.com/services/T/B/x",
                events = listOf("link.created"),
                isActive = false,
                payloadFormat = "slack",
            ),
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"is_active\":false"))
        assertTrue(body.contains("\"payload_format\":\"slack\""))
    }

    @Test
    fun `create surfaces a server error`() {
        server.enqueue(jsonResponse("""{"code":"bad_request","message":"bad url"}""", status = 422))
        val ex = assertThrows(ReroutException::class.java) {
            runTest {
                server.client().webhooks.create(
                    CreateWebhookInput(name = "x", url = "x", events = listOf("link.created")),
                )
            }
        }
        assertEquals("bad_request", ex.code)
        assertEquals(422, ex.status)
    }

    // ─── list ───────────────────────────────────────────────────────────────

    @Test
    fun `list parses endpoints and event types`() = runTest {
        server.enqueue(
            jsonResponse(
                """{"endpoints":[$SAMPLE_WEBHOOK_JSON],"event_types":["link.created","link.clicked","domain.verified"]}""",
            ),
        )
        val result = server.client().webhooks.list()
        assertEquals(1, result.endpoints.size)
        assertEquals("wh_abc123", result.endpoints[0].id)
        assertEquals("https://example.com/hooks/rerout", result.endpoints[0].url)
        assertTrue(result.eventTypes.contains("domain.verified"))
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/projects/me/webhooks", recorded.path)
    }

    @Test
    fun `list tolerates an empty endpoints array`() = runTest {
        server.enqueue(jsonResponse("""{"endpoints":[],"event_types":["link.created"]}"""))
        val result = server.client().webhooks.list()
        assertTrue(result.endpoints.isEmpty())
        assertEquals(listOf("link.created"), result.eventTypes)
    }

    @Test
    fun `list surfaces an auth error`() {
        server.enqueue(jsonResponse("""{"code":"unauthorized","message":"bad key"}""", status = 401))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().webhooks.list() }
        }
        assertEquals("unauthorized", ex.code)
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Test
    fun `delete sends DELETE to the endpoint id and returns the deleted flag`() = runTest {
        server.enqueue(jsonResponse("""{"deleted":true}"""))
        val result = server.client().webhooks.delete("wh_abc123")
        assertTrue(result.deleted)
        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/v1/projects/me/webhooks/wh_abc123", recorded.path)
    }

    @Test
    fun `delete tolerates an empty success body`() = runTest {
        server.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(204))
        val result = server.client().webhooks.delete("wh_abc123")
        assertTrue(result.deleted)
    }

    @Test
    fun `delete surfaces a forbidden error`() {
        server.enqueue(jsonResponse("""{"code":"forbidden","message":"not yours"}""", status = 403))
        val ex = assertThrows(ReroutException::class.java) {
            runTest { server.client().webhooks.delete("wh_abc123") }
        }
        assertEquals("forbidden", ex.code)
    }

    // ─── URL encoding ───────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource(
        "wh_abc123, /v1/projects/me/webhooks/wh_abc123",
        "wh a/b, /v1/projects/me/webhooks/wh%20a%2Fb",
    )
    fun `endpoint ids are url-encoded as a single path segment`(id: String, expectedPath: String) {
        server.enqueue(jsonResponse("""{"deleted":true}"""))
        runTest { server.client().webhooks.delete(id) }
        assertEquals(expectedPath, server.takeRequest().path)
    }
}
