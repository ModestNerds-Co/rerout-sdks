/*
 * rerout-kotlin — webhook signature verification tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhooksTest {
    private val secret = "whsec_test_secret"
    private val body = """{"event":"link.clicked","code":"q4"}"""
    private val fixedNow = 1_716_000_000L

    /** Compute a real `t=…,v1=…` header for [body] under [secret] at [ts]. */
    private fun signHeader(
        ts: Long = fixedNow,
        signingSecret: String = secret,
        payload: String = body,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hex = mac.doFinal("$ts.$payload".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "t=$ts,v1=$hex"
    }

    private fun clock(at: Long = fixedNow): () -> Long = { at }

    // ─── valid ──────────────────────────────────────────────────────────────

    @Test
    fun `a freshly signed payload verifies`() {
        assertTrue(
            verifyReroutSignature(body, signHeader(), secret, now = clock()),
        )
    }

    @Test
    fun `the object form verifies a fresh payload`() {
        assertTrue(
            ReroutWebhooks.verifySignature(body, signHeader(), secret, now = clock()),
        )
    }

    // ─── wrong / tampered ───────────────────────────────────────────────────

    @Test
    fun `a signature made with a different secret is rejected`() {
        val header = signHeader(signingSecret = "whsec_other")
        assertFalse(verifyReroutSignature(body, header, secret, now = clock()))
    }

    @Test
    fun `a tampered body is rejected`() {
        val header = signHeader()
        assertFalse(verifyReroutSignature("$body ", header, secret, now = clock()))
    }

    @Test
    fun `a body with a missing character is rejected`() {
        val header = signHeader()
        assertFalse(verifyReroutSignature(body.dropLast(1), header, secret, now = clock()))
    }

    // ─── timestamp tolerance ────────────────────────────────────────────────

    @Test
    fun `a payload older than the tolerance window is rejected`() {
        val header = signHeader(ts = fixedNow - 600)
        assertFalse(verifyReroutSignature(body, header, secret, now = clock()))
    }

    @Test
    fun `a payload from the future beyond tolerance is rejected`() {
        val header = signHeader(ts = fixedNow + 600)
        assertFalse(verifyReroutSignature(body, header, secret, now = clock()))
    }

    @Test
    fun `a payload exactly at the tolerance boundary is accepted`() {
        val header = signHeader(ts = fixedNow - 300)
        assertTrue(verifyReroutSignature(body, header, secret, now = clock()))
    }

    @Test
    fun `a payload one second past the boundary is rejected`() {
        val header = signHeader(ts = fixedNow - 301)
        assertFalse(verifyReroutSignature(body, header, secret, now = clock()))
    }

    @Test
    fun `tolerance zero disables the timestamp check`() {
        val header = signHeader(ts = fixedNow - 100_000)
        assertTrue(
            verifyReroutSignature(body, header, secret, toleranceSeconds = 0, now = clock()),
        )
    }

    @Test
    fun `a custom tolerance window is honoured`() {
        val header = signHeader(ts = fixedNow - 50)
        assertTrue(
            verifyReroutSignature(body, header, secret, toleranceSeconds = 60, now = clock()),
        )
        assertFalse(
            verifyReroutSignature(body, header, secret, toleranceSeconds = 30, now = clock()),
        )
    }

    // ─── malformed headers ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "garbage",
            "t=,v1=abc123",
            "v1=abc123",
            "t=1716000000",
            "t=notanumber,v1=abc123",
            "t=1716000000,v1=zzzz",
            "t=1716000000,v1=abc",
            "t=-5,v1=abcd",
            "t=0,v1=abcd",
        ],
    )
    fun `malformed headers are rejected`(header: String) {
        assertFalse(
            verifyReroutSignature(body, header, secret, toleranceSeconds = 0, now = clock()),
        )
    }

    @Test
    fun `a null header is rejected`() {
        assertFalse(verifyReroutSignature(body, null, secret, now = clock()))
    }

    // ─── header casing ──────────────────────────────────────────────────────

    @Test
    fun `uppercase header keys are accepted`() {
        val signed = signHeader()
        val upper = signed.replace("t=", "T=").replace("v1=", "V1=")
        assertTrue(verifyReroutSignature(body, upper, secret, now = clock()))
    }

    @Test
    fun `mixed-case and padded header keys are accepted`() {
        val signed = signHeader()
        val parts = signed.split(",")
        val padded = " T = ${parts[0].substringAfter("=")} , V1 = ${parts[1].substringAfter("=")} "
        assertTrue(verifyReroutSignature(body, padded, secret, now = clock()))
    }

    // ─── secret ─────────────────────────────────────────────────────────────

    @Test
    fun `an empty secret is rejected`() {
        assertFalse(verifyReroutSignature(body, signHeader(), "", now = clock()))
    }

    @Test
    fun `a null secret is rejected`() {
        assertFalse(verifyReroutSignature(body, signHeader(), null, now = clock()))
    }

    // ─── extra segments ─────────────────────────────────────────────────────

    @Test
    fun `unknown extra segments in the header are ignored`() {
        val header = "${signHeader()},v0=legacy,foo=bar"
        assertTrue(verifyReroutSignature(body, header, secret, now = clock()))
    }

    @Test
    fun `the default tolerance is five minutes`() {
        assertTrue(ReroutWebhooks.DEFAULT_TOLERANCE_SECONDS == 300L)
    }
}
