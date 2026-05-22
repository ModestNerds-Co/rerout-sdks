/*
 * rerout-spring-boot-starter — webhook controller tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.spring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookControllerTest {
    private val secret = "whsec_test_secret"
    private val handler = RecordingHandler()

    @BeforeEach
    fun reset() {
        handler.events.clear()
    }

    private val runner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ReroutAutoConfiguration::class.java))
        .withBean("recordingHandler", ReroutWebhookHandler::class.java, { handler })
        .withPropertyValues("rerout.webhook.secret=$secret")

    /** Sign [body] the way Rerout does and return the `X-Rerout-Signature` value. */
    private fun sign(body: String, ts: Long = nowSeconds(), signingSecret: String = secret): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hex = mac.doFinal("$ts.$body".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "t=$ts,v1=$hex"
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    /** Invoke the controller directly with a body + optional signature. */
    private fun post(body: String, signature: String?): Int {
        var status = -1
        runner.run { context ->
            val controller = context.getBean(ReroutWebhookController::class.java)
            val response = controller.receive(body, signature)
            status = response.statusCode.value()
        }
        return status
    }

    // ─── happy path ─────────────────────────────────────────────────────────

    @Test
    fun `a valid signed link-clicked webhook is accepted and dispatched`() {
        val body = """{"type":"link.clicked","created_at":1716000000,"data":{"code":"q4","country":"ZA","qr":true}}"""
        val status = post(body, sign(body))
        assertEquals(200, status)
        assertEquals(1, handler.events.size)
        val event = handler.events.single()
        assertTrue(event is ReroutEvent.LinkClicked)
        event as ReroutEvent.LinkClicked
        assertEquals("q4", event.code)
        assertEquals("ZA", event.country)
        assertTrue(event.qr)
        assertEquals(1716000000L, event.createdAt)
    }

    @Test
    fun `a link-created webhook parses into the typed event`() {
        val body = """{"type":"link.created","data":{"code":"sale","short_url":"https://go.brand.com/sale"}}"""
        val status = post(body, sign(body))
        assertEquals(200, status)
        val event = handler.events.single()
        assertTrue(event is ReroutEvent.LinkCreated)
        event as ReroutEvent.LinkCreated
        assertEquals("sale", event.code)
        assertEquals("https://go.brand.com/sale", event.shortUrl)
    }

    @Test
    fun `an unknown event type surfaces as ReroutEvent Unknown`() {
        val body = """{"type":"link.archived","data":{"code":"old"}}"""
        val status = post(body, sign(body))
        assertEquals(200, status)
        val event = handler.events.single()
        assertTrue(event is ReroutEvent.Unknown)
        event as ReroutEvent.Unknown
        assertEquals("link.archived", event.type)
    }

    // ─── rejection paths ────────────────────────────────────────────────────

    @Test
    fun `a missing signature header is rejected with 400`() {
        val body = """{"type":"link.clicked","data":{"code":"q4"}}"""
        assertEquals(400, post(body, null))
        assertTrue(handler.events.isEmpty())
    }

    @Test
    fun `a signature made with the wrong secret is rejected with 400`() {
        val body = """{"type":"link.clicked","data":{"code":"q4"}}"""
        val badSig = sign(body, signingSecret = "whsec_wrong")
        assertEquals(400, post(body, badSig))
        assertTrue(handler.events.isEmpty())
    }

    @Test
    fun `a tampered body is rejected with 400`() {
        val body = """{"type":"link.clicked","data":{"code":"q4"}}"""
        val sig = sign(body)
        assertEquals(400, post("$body ", sig))
        assertTrue(handler.events.isEmpty())
    }

    @Test
    fun `a stale signature is rejected with 400`() {
        val body = """{"type":"link.clicked","data":{"code":"q4"}}"""
        val staleSig = sign(body, ts = nowSeconds() - 1000)
        assertEquals(400, post(body, staleSig))
        assertTrue(handler.events.isEmpty())
    }

    @Test
    fun `a malformed signature header is rejected with 400`() {
        val body = """{"type":"link.clicked","data":{"code":"q4"}}"""
        assertEquals(400, post(body, "garbage"))
    }

    @Test
    fun `a verified but unparseable body is rejected with 400`() {
        val body = "not json at all"
        assertEquals(400, post(body, sign(body)))
        assertTrue(handler.events.isEmpty())
    }

    @Test
    fun `a verified body missing the type field is rejected with 400`() {
        val body = """{"data":{"code":"q4"}}"""
        assertEquals(400, post(body, sign(body)))
    }

    // ─── dispatch ───────────────────────────────────────────────────────────

    @Test
    fun `every registered handler receives the event`() {
        val second = RecordingHandler()
        var status = -1
        WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReroutAutoConfiguration::class.java))
            .withPropertyValues("rerout.webhook.secret=$secret")
            .withBean("h1", ReroutWebhookHandler::class.java, { handler })
            .withBean("h2", ReroutWebhookHandler::class.java, { second })
            .run { context ->
                val body = """{"type":"link.clicked","data":{"code":"q4"}}"""
                val controller = context.getBean(ReroutWebhookController::class.java)
                status = controller.receive(body, sign(body)).statusCode.value()
            }
        assertEquals(200, status)
        assertEquals(1, handler.events.size)
        assertEquals(1, second.events.size)
    }

    @Test
    fun `the controller works with no handlers registered`() {
        var status = -1
        WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReroutAutoConfiguration::class.java))
            .withPropertyValues("rerout.webhook.secret=$secret")
            .run { context ->
                val body = """{"type":"link.clicked","data":{"code":"q4"}}"""
                val controller = context.getBean(ReroutWebhookController::class.java)
                status = controller.receive(body, sign(body)).statusCode.value()
            }
        assertEquals(200, status)
    }

    /** A [ReroutWebhookHandler] that records everything it receives. */
    class RecordingHandler : ReroutWebhookHandler {
        val events: MutableList<ReroutEvent> = mutableListOf()

        override fun onEvent(event: ReroutEvent) {
            events.add(event)
        }
    }
}
