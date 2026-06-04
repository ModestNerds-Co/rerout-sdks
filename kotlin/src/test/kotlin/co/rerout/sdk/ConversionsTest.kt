/*
 * rerout-kotlin — conversions namespace tests.
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

class ConversionsTest {
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

    @Test
    fun `record posts to the conversions endpoint with the right body`() = runTest {
        server.enqueue(jsonResponse("""{"recorded":true,"duplicate":false}"""))
        val result = server.client().conversions.record(
            RecordConversionInput(
                clickId = "rrid_123",
                eventName = "purchase",
                valueCents = 1999,
                currency = "USD",
            ),
        )
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/conversions", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"click_id\":\"rrid_123\""))
        assertTrue(body.contains("\"event_name\":\"purchase\""))
        assertTrue(body.contains("\"value_cents\":1999"))
        assertTrue(body.contains("\"currency\":\"USD\""))

        assertTrue(result.recorded)
        assertFalse(result.duplicate)
    }

    @Test
    fun `record omits the optional fields when not set`() = runTest {
        server.enqueue(jsonResponse("""{"recorded":true,"duplicate":false}"""))
        server.client().conversions.record(
            RecordConversionInput(clickId = "rrid_123", eventName = "signup"),
        )
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"click_id\":\"rrid_123\""))
        assertTrue(body.contains("\"event_name\":\"signup\""))
        assertFalse(body.contains("value_cents"))
        assertFalse(body.contains("currency"))
    }

    @Test
    fun `record surfaces the duplicate flag on a repeat`() = runTest {
        server.enqueue(jsonResponse("""{"recorded":true,"duplicate":true}"""))
        val result = server.client().conversions.record(
            RecordConversionInput(clickId = "rrid_123", eventName = "purchase"),
        )
        assertTrue(result.recorded)
        assertTrue(result.duplicate)
    }

    @Test
    fun `record surfaces a server error`() {
        server.enqueue(jsonResponse("""{"code":"not_found","message":"no such click"}""", status = 404))
        val ex = assertThrows(ReroutException::class.java) {
            runTest {
                server.client().conversions.record(
                    RecordConversionInput(clickId = "rrid_nope", eventName = "purchase"),
                )
            }
        }
        assertEquals("not_found", ex.code)
        assertEquals(404, ex.status)
    }
}
