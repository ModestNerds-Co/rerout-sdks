/*
 * rerout-kotlin — Official Kotlin/JVM SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Helper for verifying inbound `X-Rerout-Signature` webhook headers.
 *
 * Rerout signs every webhook delivery as
 * `t={unix_seconds},v1={hex_hmac_sha256}` where the HMAC is computed over
 * `"{timestamp}.{raw_body}"` with the endpoint signing secret as the key.
 *
 * ## Usage
 *
 * ```kotlin
 * import co.rerout.sdk.ReroutWebhooks
 *
 * val ok = ReroutWebhooks.verifySignature(
 *     rawBody = rawBody,
 *     signatureHeader = request.getHeader("X-Rerout-Signature"),
 *     secret = System.getenv("REROUT_WEBHOOK_SECRET"),
 * )
 * if (!ok) return ResponseEntity.badRequest().build()
 * ```
 */
public object ReroutWebhooks {
    /**
     * Default tolerance window in seconds between the `t=` timestamp and the
     * current time. Five minutes — protects against captured-replay attacks.
     */
    public const val DEFAULT_TOLERANCE_SECONDS: Long = 300

    /**
     * Returns `true` when [signatureHeader] is a valid Rerout HMAC signature
     * for [rawBody] under [secret].
     *
     * Returns `false` when the header is missing, malformed, the timestamp is
     * outside the tolerance window, or the HMAC doesn't match.
     *
     * @param rawBody the raw, unmodified request body.
     * @param signatureHeader the value of the `X-Rerout-Signature` header.
     * @param secret the endpoint signing secret (`whsec_…`).
     * @param toleranceSeconds staleness window; pass `0` to disable the
     *   timestamp check. Defaults to [DEFAULT_TOLERANCE_SECONDS].
     * @param now injectable clock returning the current unix time in seconds —
     *   useful for deterministic tests. Defaults to the system clock.
     */
    @JvmStatic
    @JvmOverloads
    public fun verifySignature(
        rawBody: String,
        signatureHeader: String?,
        secret: String?,
        toleranceSeconds: Long = DEFAULT_TOLERANCE_SECONDS,
        now: () -> Long = { System.currentTimeMillis() / 1000 },
    ): Boolean {
        if (signatureHeader.isNullOrEmpty() || secret.isNullOrEmpty()) return false

        val parsed = parseHeader(signatureHeader) ?: return false

        if (toleranceSeconds > 0) {
            val delta = now() - parsed.timestamp
            if (kotlin.math.abs(delta) > toleranceSeconds) return false
        }

        val expected = hmacSha256(secret, "${parsed.timestamp}.$rawBody")
        val actual = decodeHex(parsed.v1) ?: return false
        if (expected.size != actual.size) return false
        return constantTimeEquals(expected, actual)
    }

    private data class ParsedHeader(val timestamp: Long, val v1: String)

    private fun parseHeader(header: String): ParsedHeader? {
        var timestamp: Long? = null
        var v1: String? = null
        for (segment in header.split(',')) {
            val eq = segment.indexOf('=')
            if (eq <= 0) continue
            val key = segment.substring(0, eq).trim().lowercase()
            val value = segment.substring(eq + 1).trim()
            when (key) {
                "t" -> {
                    val parsed = value.toLongOrNull()
                    if (parsed != null && parsed > 0) timestamp = parsed
                }
                "v1" -> if (value.isNotEmpty()) v1 = value
            }
        }
        val ts = timestamp ?: return null
        val sig = v1 ?: return null
        return ParsedHeader(ts, sig)
    }

    private fun hmacSha256(secret: String, payload: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8))
    }

    private fun decodeHex(hex: String): ByteArray? {
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = hexDigit(hex[i * 2])
            val lo = hexDigit(hex[i * 2 + 1])
            if (hi < 0 || lo < 0) return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    private fun hexDigit(c: Char): Int = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> -1
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }
}

/**
 * Top-level convenience wrapper around [ReroutWebhooks.verifySignature], to
 * match the free-function style used by other Rerout SDKs.
 */
@JvmOverloads
public fun verifyReroutSignature(
    rawBody: String,
    signatureHeader: String?,
    secret: String?,
    toleranceSeconds: Long = ReroutWebhooks.DEFAULT_TOLERANCE_SECONDS,
    now: () -> Long = { System.currentTimeMillis() / 1000 },
): Boolean = ReroutWebhooks.verifySignature(rawBody, signatureHeader, secret, toleranceSeconds, now)
