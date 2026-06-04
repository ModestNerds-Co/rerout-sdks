/*
 * rerout-kotlin — Smart Links field + batch create tests.
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SmartLinksTest {
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

    // ─── Link response parsing ────────────────────────────────────────────────

    @Test
    fun `link parses the smart-links fields`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_SMART_LINK_JSON))
        val link = server.client().links.get("vip")
        assertTrue(link.passwordProtected)
        assertEquals(1000L, link.maxClicks)
        assertEquals(42L, link.clickCount)
        assertTrue(link.trackConversions)
        assertEquals(2, link.routingRules.size)
        assertEquals("country", link.routingRules[0].conditionType)
        assertEquals("is", link.routingRules[0].conditionOp)
        assertEquals("ZA", link.routingRules[0].conditionValue)
        assertEquals("https://example.com/za", link.routingRules[0].targetUrl)
        assertEquals("device", link.routingRules[1].conditionType)
        assertEquals("in", link.routingRules[1].conditionOp)
        assertEquals(2, link.abVariants.size)
        assertEquals(1L, link.abVariants[0].id)
        assertEquals("https://example.com/a", link.abVariants[0].targetUrl)
        assertEquals(60, link.abVariants[0].weight)
    }

    @Test
    fun `link defaults the smart-links fields when the server omits them`() = runTest {
        // The legacy SAMPLE_LINK_JSON has none of the smart-links keys.
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        val link = server.client().links.get("q4")
        assertFalse(link.passwordProtected)
        assertNull(link.maxClicks)
        assertEquals(0L, link.clickCount)
        assertFalse(link.trackConversions)
        assertTrue(link.routingRules.isEmpty())
        assertTrue(link.abVariants.isEmpty())
    }

    // ─── create with smart-links input ────────────────────────────────────────

    @Test
    fun `create serializes the smart-links input fields`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_SMART_LINK_JSON))
        server.client().links.create(
            CreateLinkInput(
                targetUrl = "https://example.com/default",
                password = "hunter2",
                maxClicks = 1000,
                trackConversions = true,
                routingRules = listOf(
                    RoutingRule(
                        conditionType = "country",
                        conditionOp = "is",
                        conditionValue = "ZA",
                        targetUrl = "https://example.com/za",
                    ),
                ),
                abVariants = listOf(
                    AbVariantInput(targetUrl = "https://example.com/a", weight = 60),
                    AbVariantInput(targetUrl = "https://example.com/b"),
                ),
            ),
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"password\":\"hunter2\""))
        assertTrue(body.contains("\"max_clicks\":1000"))
        assertTrue(body.contains("\"track_conversions\":true"))
        assertTrue(body.contains("\"condition_type\":\"country\""))
        assertTrue(body.contains("\"condition_op\":\"is\""))
        assertTrue(body.contains("\"condition_value\":\"ZA\""))
        assertTrue(body.contains("\"target_url\":\"https://example.com/za\""))
        assertTrue(body.contains("\"target_url\":\"https://example.com/a\""))
        assertTrue(body.contains("\"weight\":60"))
        // The variant with no weight omits the field.
        assertTrue(body.contains("\"target_url\":\"https://example.com/b\""))
    }

    @Test
    fun `create omits smart-links fields that are not set`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_LINK_JSON))
        server.client().links.create(CreateLinkInput(targetUrl = "https://example.com"))
        val body = server.takeRequest().body.readUtf8()
        assertFalse(body.contains("password"))
        assertFalse(body.contains("max_clicks"))
        assertFalse(body.contains("track_conversions"))
        assertFalse(body.contains("routing_rules"))
        assertFalse(body.contains("ab_variants"))
    }

    // ─── update with smart-links input ────────────────────────────────────────

    @Test
    fun `update serializes routing rules and ab variants as a full replace`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_SMART_LINK_JSON))
        server.client().links.update(
            "vip",
            UpdateLinkInput.builder()
                .trackConversions(true)
                .routingRules(
                    listOf(
                        RoutingRule(
                            conditionType = "device",
                            conditionOp = "is_not",
                            conditionValue = "bot",
                            targetUrl = "https://example.com/human",
                        ),
                    ),
                )
                .abVariants(listOf(AbVariantInput(targetUrl = "https://example.com/v2", weight = 100)))
                .build(),
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"track_conversions\":true"))
        assertTrue(body.contains("\"condition_op\":\"is_not\""))
        assertTrue(body.contains("\"routing_rules\":["))
        assertTrue(body.contains("\"ab_variants\":["))
        assertTrue(body.contains("\"weight\":100"))
    }

    @Test
    fun `update can set a password and a max clicks cap`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_SMART_LINK_JSON))
        server.client().links.update(
            "vip",
            UpdateLinkInput.builder().password("s3cret").maxClicks(500).build(),
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"password\":\"s3cret\""))
        assertTrue(body.contains("\"max_clicks\":500"))
    }

    @Test
    fun `update sends explicit null when clearing password and max clicks`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_SMART_LINK_JSON))
        server.client().links.update(
            "vip",
            UpdateLinkInput.builder().clearPassword().clearMaxClicks().build(),
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"password\":null"))
        assertTrue(body.contains("\"max_clicks\":null"))
    }

    @Test
    fun `update can clear routing rules with an empty list`() = runTest {
        server.enqueue(jsonResponse(SAMPLE_SMART_LINK_JSON))
        server.client().links.update(
            "vip",
            UpdateLinkInput.builder().routingRules(emptyList()).build(),
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"routing_rules\":[]"))
    }

    @Test
    fun `update keeps a real max clicks value over the clear flag`() {
        val input = UpdateLinkInput.builder().maxClicks(750).build()
        assertEquals(750L, input.toJsonMap()["max_clicks"])
    }

    // ─── batch create ─────────────────────────────────────────────────────────

    @Test
    fun `create batch posts the wrapped links body`() = runTest {
        server.enqueue(
            jsonResponse(
                """
                {
                  "created": 2,
                  "total": 3,
                  "results": [
                    {"index": 0, "ok": true, "code": "a1"},
                    {"index": 1, "ok": true, "code": "b2"},
                    {"index": 2, "ok": false, "error": "bad_target_url"}
                  ]
                }
                """,
            ),
        )
        val result = server.client().links.createBatch(
            listOf(
                BatchLinkInput(targetUrl = "https://example.com/1"),
                BatchLinkInput(targetUrl = "https://example.com/2", code = "b2"),
                BatchLinkInput(targetUrl = "nope"),
            ),
        )
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/links/batch", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"links\":["))
        assertTrue(body.contains("\"target_url\":\"https://example.com/1\""))
        assertTrue(body.contains("\"code\":\"b2\""))

        assertEquals(2, result.created)
        assertEquals(3, result.total)
        assertEquals(3, result.results.size)
        assertEquals(0, result.results[0].index)
        assertTrue(result.results[0].ok)
        assertEquals("a1", result.results[0].code)
        assertFalse(result.results[2].ok)
        assertEquals("bad_target_url", result.results[2].error)
    }

    @Test
    fun `create batch surfaces a server error`() {
        server.enqueue(jsonResponse("""{"code":"rate_limited","message":"slow down"}""", status = 429))
        val ex = assertThrows(ReroutException::class.java) {
            runTest {
                server.client().links.createBatch(
                    listOf(BatchLinkInput(targetUrl = "https://example.com")),
                )
            }
        }
        assertEquals("rate_limited", ex.code)
        assertTrue(ex.isRateLimited)
    }
}
