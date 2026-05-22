<?php

declare(strict_types=1);

namespace Rerout\Tests;

use PHPUnit\Framework\Attributes\CoversClass;
use PHPUnit\Framework\Attributes\DataProvider;
use PHPUnit\Framework\TestCase as BaseTestCase;
use Rerout\Webhooks\SignatureVerifier;

/**
 * Coverage for {@see SignatureVerifier} — Rerout webhook HMAC verification.
 */
#[CoversClass(SignatureVerifier::class)]
final class WebhooksTest extends BaseTestCase
{
    private const SECRET = 'whsec_test_secret';

    private const BODY = '{"event":"link.clicked","data":{"code":"q4"}}';

    /**
     * Build a valid `t=…,v1=…` header for the given body/secret/timestamp.
     */
    private static function sign(string $body, string $secret, int $timestamp): string
    {
        $hmac = hash_hmac('sha256', $timestamp . '.' . $body, $secret);

        return "t={$timestamp},v1={$hmac}";
    }

    // ─── Valid signatures ───────────────────────────────────────────────────

    public function testValidFreshlySignedSignatureIsAccepted(): void
    {
        $now = 1_700_000_000;
        $header = self::sign(self::BODY, self::SECRET, $now);

        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testEmptyBodyCanBeSignedAndVerified(): void
    {
        $now = 1_700_000_000;
        $header = self::sign('', self::SECRET, $now);

        self::assertTrue(SignatureVerifier::verify(
            rawBody: '',
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testDefaultToleranceConstantIsThreeHundredSeconds(): void
    {
        self::assertSame(300, SignatureVerifier::DEFAULT_TOLERANCE_SECONDS);
    }

    // ─── Rejections ─────────────────────────────────────────────────────────

    public function testWrongSecretIsRejected(): void
    {
        $now = 1_700_000_000;
        $header = self::sign(self::BODY, 'whsec_different_secret', $now);

        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testTamperedBodyIsRejected(): void
    {
        $now = 1_700_000_000;
        $header = self::sign(self::BODY, self::SECRET, $now);

        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY . ' ', // an extra trailing space
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testExpiredSignatureOutsideToleranceIsRejected(): void
    {
        $signedAt = 1_700_000_000;
        $header = self::sign(self::BODY, self::SECRET, $signedAt);

        // 301 seconds later — one second past the default 300s window.
        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $signedAt + 301,
        ));
    }

    public function testFutureSignatureOutsideToleranceIsRejected(): void
    {
        $signedAt = 1_700_000_000;
        $header = self::sign(self::BODY, self::SECRET, $signedAt);

        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $signedAt - 301,
        ));
    }

    public function testSignatureExactlyAtToleranceBoundaryIsAccepted(): void
    {
        $signedAt = 1_700_000_000;
        $header = self::sign(self::BODY, self::SECRET, $signedAt);

        // Exactly 300 seconds — abs(diff) == tolerance, must be accepted.
        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $signedAt + 300,
        ));
        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $signedAt - 300,
        ));
    }

    public function testCustomToleranceWindowIsHonoured(): void
    {
        $signedAt = 1_700_000_000;
        $header = self::sign(self::BODY, self::SECRET, $signedAt);

        // 60s window; signed 90s ago → rejected.
        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            toleranceSeconds: 60,
            clock: static fn (): int => $signedAt + 90,
        ));
        // 600s window; signed 90s ago → accepted.
        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            toleranceSeconds: 600,
            clock: static fn (): int => $signedAt + 90,
        ));
    }

    public function testToleranceZeroDisablesTimestampCheck(): void
    {
        // Signed in the distant past; tolerance 0 must skip the time check
        // entirely and verify on HMAC alone.
        $signedAt = 1;
        $header = self::sign(self::BODY, self::SECRET, $signedAt);

        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            toleranceSeconds: 0,
            clock: static fn (): int => 1_700_000_000,
        ));
    }

    public function testToleranceZeroStillRejectsBadHmac(): void
    {
        $header = self::sign(self::BODY, 'whsec_wrong', 1);

        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            toleranceSeconds: 0,
        ));
    }

    public function testEmptySecretIsRejected(): void
    {
        $header = self::sign(self::BODY, self::SECRET, 1_700_000_000);

        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: '',
        ));
    }

    public function testEmptyHeaderIsRejected(): void
    {
        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: '',
            secret: self::SECRET,
        ));
    }

    // ─── Malformed headers ──────────────────────────────────────────────────

    /**
     * @return array<string, array{0: string}>
     */
    public static function malformedHeaderProvider(): array
    {
        return [
            'empty string' => [''],
            'pure garbage' => ['garbage'],
            'empty t and v1' => ['t=,v1=abc'],
            'missing t' => ['v1=abc123'],
            'missing v1' => ['t=1700000000'],
            'non-numeric t' => ['t=notanumber,v1=abcdef'],
            'non-hex v1' => ['t=1700000000,v1=zzzzzz'],
            'odd-length v1' => ['t=1700000000,v1=abc'],
            'negative t' => ['t=-5,v1=abcdef'],
            'zero t' => ['t=0,v1=abcdef'],
            'no equals signs' => ['t1700000000v1abc'],
            'empty v1 value' => ['t=1700000000,v1='],
        ];
    }

    #[DataProvider('malformedHeaderProvider')]
    public function testMalformedHeadersAreRejected(string $header): void
    {
        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => 1_700_000_000,
        ));
    }

    // ─── Casing variations ──────────────────────────────────────────────────

    public function testUppercaseKeysAreAccepted(): void
    {
        $now = 1_700_000_000;
        $hmac = hash_hmac('sha256', $now . '.' . self::BODY, self::SECRET);
        $header = "T={$now},V1={$hmac}";

        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testMixedCaseKeysAreAccepted(): void
    {
        $now = 1_700_000_000;
        $hmac = hash_hmac('sha256', $now . '.' . self::BODY, self::SECRET);
        $header = "T={$now},v1={$hmac}";

        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testUppercaseHexInV1IsAccepted(): void
    {
        $now = 1_700_000_000;
        $hmac = strtoupper(hash_hmac('sha256', $now . '.' . self::BODY, self::SECRET));
        $header = "t={$now},v1={$hmac}";

        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testWhitespacePaddedSegmentsAreAccepted(): void
    {
        $now = 1_700_000_000;
        $hmac = hash_hmac('sha256', $now . '.' . self::BODY, self::SECRET);
        $header = " t = {$now} , v1 = {$hmac} ";

        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testExtraSegmentsAreIgnored(): void
    {
        $now = 1_700_000_000;
        $hmac = hash_hmac('sha256', $now . '.' . self::BODY, self::SECRET);
        $header = "t={$now},v1={$hmac},v2=futureproof";

        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testWrongLengthV1IsRejected(): void
    {
        $now = 1_700_000_000;
        // Valid hex, even length, but not 64 chars (32 bytes) of SHA-256.
        $header = "t={$now},v1=abcdef0123456789";

        self::assertFalse(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
            clock: static fn (): int => $now,
        ));
    }

    public function testDefaultToleranceUsedWhenNotSpecified(): void
    {
        // No clock injected — uses real time(). A signature timestamped
        // "now" must verify under the default 300s window.
        $now = time();
        $header = self::sign(self::BODY, self::SECRET, $now);

        self::assertTrue(SignatureVerifier::verify(
            rawBody: self::BODY,
            signatureHeader: $header,
            secret: self::SECRET,
        ));
    }
}
