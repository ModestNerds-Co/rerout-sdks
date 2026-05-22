/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.function.LongSupplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper for verifying inbound {@code X-Rerout-Signature} webhook headers.
 *
 * <p>Rerout signs every webhook delivery as
 * {@code t={unix_seconds},v1={hex_hmac_sha256}}, where the HMAC is computed
 * over {@code "{timestamp}.{raw_body}"} with the endpoint signing secret as
 * the key.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * boolean ok = Webhooks.verifyReroutSignature(
 *     rawRequestBody,
 *     request.getHeader("X-Rerout-Signature"),
 *     System.getenv("REROUT_WEBHOOK_SECRET"));
 * if (!ok) {
 *     // reject the request
 * }
 * }</pre>
 */
public final class Webhooks {

    /**
     * Default tolerance window in seconds between the {@code t=} timestamp and
     * the current time. Five minutes — protects against captured-replay
     * attacks.
     */
    public static final int DEFAULT_TOLERANCE_SECONDS = 300;

    private Webhooks() {
    }

    /**
     * Verifies a Rerout webhook signature, using the default 300-second
     * tolerance and the system clock.
     *
     * @param rawBody         the raw, unmodified request body
     * @param signatureHeader the value of the {@code X-Rerout-Signature} header
     * @param secret          the endpoint signing secret ({@code whsec_…})
     * @return {@code true} when the signature is valid, {@code false} otherwise
     */
    public static boolean verifyReroutSignature(
            String rawBody,
            String signatureHeader,
            String secret) {
        return verifyReroutSignature(
                rawBody, signatureHeader, secret, DEFAULT_TOLERANCE_SECONDS, null);
    }

    /**
     * Verifies a Rerout webhook signature with a custom tolerance, using the
     * system clock.
     *
     * @param rawBody          the raw, unmodified request body
     * @param signatureHeader  the value of the {@code X-Rerout-Signature} header
     * @param secret           the endpoint signing secret ({@code whsec_…})
     * @param toleranceSeconds the staleness window; pass {@code 0} to disable
     *                         the timestamp check
     * @return {@code true} when the signature is valid, {@code false} otherwise
     */
    public static boolean verifyReroutSignature(
            String rawBody,
            String signatureHeader,
            String secret,
            int toleranceSeconds) {
        return verifyReroutSignature(
                rawBody, signatureHeader, secret, toleranceSeconds, null);
    }

    /**
     * Verifies a Rerout webhook signature.
     *
     * <p>Returns {@code true} only when the header parses cleanly, the
     * timestamp is within {@code toleranceSeconds} of {@code clock} (when the
     * check is enabled), and the recomputed HMAC matches the supplied
     * {@code v1} in constant time.
     *
     * <p>Returns {@code false} when the header or secret is empty, the header
     * is malformed (missing {@code t}/{@code v1}, a non-numeric or non-positive
     * {@code t}, or a non-hex / odd-length {@code v1}), the timestamp is
     * outside tolerance, or the HMAC does not match.
     *
     * @param rawBody          the raw, unmodified request body
     * @param signatureHeader  the value of the {@code X-Rerout-Signature}
     *                         header; may be {@code null}
     * @param secret           the endpoint signing secret ({@code whsec_…});
     *                         may be {@code null}
     * @param toleranceSeconds the staleness window in seconds; pass {@code 0}
     *                         to disable the timestamp check
     * @param clock            an injectable clock returning the current unix
     *                         time in seconds — useful for deterministic
     *                         tests; pass {@code null} to use the system clock
     * @return {@code true} when the signature is valid, {@code false} otherwise
     */
    public static boolean verifyReroutSignature(
            String rawBody,
            String signatureHeader,
            String secret,
            int toleranceSeconds,
            LongSupplier clock) {
        if (rawBody == null
                || signatureHeader == null || signatureHeader.isEmpty()
                || secret == null || secret.isEmpty()) {
            return false;
        }

        ParsedHeader parsed = parseHeader(signatureHeader);
        if (parsed == null) {
            return false;
        }

        if (toleranceSeconds > 0) {
            long now = clock != null
                    ? clock.getAsLong()
                    : System.currentTimeMillis() / 1000L;
            long delta = Math.abs(now - parsed.timestamp);
            if (delta > toleranceSeconds) {
                return false;
            }
        }

        byte[] expected = hmacSha256(secret, parsed.timestamp + "." + rawBody);
        byte[] actual = decodeHex(parsed.v1);
        if (actual == null || expected.length != actual.length) {
            return false;
        }
        return MessageDigest.isEqual(expected, actual);
    }

    private static final class ParsedHeader {
        final long timestamp;
        final String v1;

        ParsedHeader(long timestamp, String v1) {
            this.timestamp = timestamp;
            this.v1 = v1;
        }
    }

    private static ParsedHeader parseHeader(String header) {
        Long timestamp = null;
        String v1 = null;
        for (String segment : header.split(",")) {
            int eq = segment.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = segment.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = segment.substring(eq + 1).trim();
            if ("t".equals(key)) {
                try {
                    long parsed = Long.parseLong(value);
                    if (parsed > 0) {
                        timestamp = parsed;
                    }
                } catch (NumberFormatException ignored) {
                    // leave timestamp null — header is rejected below
                }
            } else if ("v1".equals(key)) {
                if (!value.isEmpty()) {
                    v1 = value;
                }
            }
        }
        if (timestamp == null || v1 == null) {
            return null;
        }
        return new ParsedHeader(timestamp, v1);
    }

    private static byte[] hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            // HmacSHA256 is mandated by the JRE; this is unreachable in practice.
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private static byte[] decodeHex(String hex) {
        if (hex.isEmpty() || hex.length() % 2 != 0) {
            return null;
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = hexDigit(hex.charAt(i * 2));
            int lo = hexDigit(hex.charAt(i * 2 + 1));
            if (hi < 0 || lo < 0) {
                return null;
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }
}
