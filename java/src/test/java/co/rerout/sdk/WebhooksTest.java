/*
 * rerout-java — webhook signature verification tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk;

import static co.rerout.sdk.Webhooks.verifyReroutSignature;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.function.LongSupplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WebhooksTest {

    private static final String SECRET = "whsec_test_secret";
    private static final String BODY = "{\"event\":\"link.clicked\",\"code\":\"q4\"}";
    private static final long FIXED_NOW = 1_716_000_000L;

    /** Computes a real {@code t=…,v1=…} header for a body under a secret. */
    private static String signHeader(long ts, String signingSecret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((ts + "." + payload).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return "t=" + ts + ",v1=" + hex;
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String signHeader() {
        return signHeader(FIXED_NOW, SECRET, BODY);
    }

    private static LongSupplier clock(long at) {
        return () -> at;
    }

    private static LongSupplier clock() {
        return clock(FIXED_NOW);
    }

    // ─── valid ──────────────────────────────────────────────────────────────

    @Test
    void aFreshlySignedPayloadVerifies() {
        assertTrue(verifyReroutSignature(
                BODY, signHeader(), SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void theDefaultToleranceConvenienceFormVerifies() {
        // System clock — sign at "now" so it falls inside the 300s window.
        long now = System.currentTimeMillis() / 1000L;
        assertTrue(verifyReroutSignature(
                BODY, signHeader(now, SECRET, BODY), SECRET));
    }

    // ─── wrong / tampered ───────────────────────────────────────────────────

    @Test
    void aSignatureMadeWithADifferentSecretIsRejected() {
        String header = signHeader(FIXED_NOW, "whsec_other", BODY);
        assertFalse(verifyReroutSignature(
                BODY, header, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void aTamperedBodyWithAnExtraSpaceIsRejected() {
        String header = signHeader();
        assertFalse(verifyReroutSignature(
                BODY + " ", header, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void aBodyWithAMissingCharacterIsRejected() {
        String header = signHeader();
        assertFalse(verifyReroutSignature(
                BODY.substring(0, BODY.length() - 1), header, SECRET,
                Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    // ─── timestamp tolerance ────────────────────────────────────────────────

    @Test
    void aPayloadOlderThanTheToleranceWindowIsRejected() {
        String header = signHeader(FIXED_NOW - 600, SECRET, BODY);
        assertFalse(verifyReroutSignature(
                BODY, header, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void aPayloadFromTheFutureBeyondToleranceIsRejected() {
        String header = signHeader(FIXED_NOW + 600, SECRET, BODY);
        assertFalse(verifyReroutSignature(
                BODY, header, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void aPayloadExactlyAtTheToleranceBoundaryIsAccepted() {
        String header = signHeader(FIXED_NOW - 300, SECRET, BODY);
        assertTrue(verifyReroutSignature(
                BODY, header, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void aPayloadOneSecondPastTheBoundaryIsRejected() {
        String header = signHeader(FIXED_NOW - 301, SECRET, BODY);
        assertFalse(verifyReroutSignature(
                BODY, header, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void toleranceZeroDisablesTheTimestampCheck() {
        String header = signHeader(FIXED_NOW - 100_000, SECRET, BODY);
        assertTrue(verifyReroutSignature(BODY, header, SECRET, 0, clock()));
    }

    @Test
    void aCustomToleranceWindowIsHonoured() {
        String header = signHeader(FIXED_NOW - 50, SECRET, BODY);
        assertTrue(verifyReroutSignature(BODY, header, SECRET, 60, clock()));
        assertFalse(verifyReroutSignature(BODY, header, SECRET, 30, clock()));
    }

    // ─── malformed headers ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
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
    })
    void malformedHeadersAreRejected(String header) {
        assertFalse(verifyReroutSignature(BODY, header, SECRET, 0, clock()));
    }

    @Test
    void aNullHeaderIsRejected() {
        assertFalse(verifyReroutSignature(
                BODY, null, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void anEmptyHeaderIsRejected() {
        assertFalse(verifyReroutSignature(
                BODY, "", SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    // ─── header casing ──────────────────────────────────────────────────────

    @Test
    void uppercaseHeaderKeysAreAccepted() {
        String upper = signHeader().replace("t=", "T=").replace("v1=", "V1=");
        assertTrue(verifyReroutSignature(
                BODY, upper, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void mixedCaseAndPaddedHeaderKeysAreAccepted() {
        String[] parts = signHeader().split(",");
        String padded = " T = " + parts[0].substring(parts[0].indexOf('=') + 1)
                + " , V1 = " + parts[1].substring(parts[1].indexOf('=') + 1) + " ";
        assertTrue(verifyReroutSignature(
                BODY, padded, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    // ─── secret ─────────────────────────────────────────────────────────────

    @Test
    void anEmptySecretIsRejected() {
        assertFalse(verifyReroutSignature(
                BODY, signHeader(), "", Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void aNullSecretIsRejected() {
        assertFalse(verifyReroutSignature(
                BODY, signHeader(), null, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    // ─── extra segments ─────────────────────────────────────────────────────

    @Test
    void unknownExtraSegmentsInTheHeaderAreIgnored() {
        String header = signHeader() + ",v0=legacy,foo=bar";
        assertTrue(verifyReroutSignature(
                BODY, header, SECRET, Webhooks.DEFAULT_TOLERANCE_SECONDS, clock()));
    }

    @Test
    void theDefaultToleranceIsFiveMinutes() {
        assertEquals(300, Webhooks.DEFAULT_TOLERANCE_SECONDS);
    }
}
