using System;
using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using Rerout.Webhooks;
using Xunit;

namespace Rerout.Tests;

public sealed class SignatureVerifierTests
{
    private const string Secret = "whsec_test_secret";
    private const string Body = """{"event":"link.clicked","code":"q4"}""";

    private static string Sign(string body, long timestamp, string secret)
    {
        var payload = Encoding.UTF8.GetBytes($"{timestamp.ToString(CultureInfo.InvariantCulture)}.{body}");
        var hash = HMACSHA256.HashData(Encoding.UTF8.GetBytes(secret), payload);
        return Convert.ToHexString(hash).ToLowerInvariant();
    }

    private static string Header(string body, long timestamp, string secret) =>
        $"t={timestamp.ToString(CultureInfo.InvariantCulture)},v1={Sign(body, timestamp, secret)}";

    [Fact]
    public void Verify_FreshlySignedPayload_ReturnsTrue()
    {
        const long ts = 1_716_200_000;
        var header = Header(Body, ts, Secret);

        var ok = SignatureVerifier.Verify(Body, header, Secret, now: () => ts);

        Assert.True(ok);
    }

    [Fact]
    public void Verify_WrongSecret_ReturnsFalse()
    {
        const long ts = 1_716_200_000;
        var header = Header(Body, ts, "whsec_other_secret");

        var ok = SignatureVerifier.Verify(Body, header, Secret, now: () => ts);

        Assert.False(ok);
    }

    [Fact]
    public void Verify_TamperedBody_ReturnsFalse()
    {
        const long ts = 1_716_200_000;
        var header = Header(Body, ts, Secret);

        var ok = SignatureVerifier.Verify(Body + " ", header, Secret, now: () => ts);

        Assert.False(ok);
    }

    [Fact]
    public void Verify_ExpiredTimestamp_ReturnsFalse()
    {
        const long ts = 1_716_200_000;
        var header = Header(Body, ts, Secret);

        // 301s drift, default tolerance is 300.
        var ok = SignatureVerifier.Verify(Body, header, Secret, now: () => ts + 301);

        Assert.False(ok);
    }

    [Fact]
    public void Verify_FutureTimestampBeyondTolerance_ReturnsFalse()
    {
        const long ts = 1_716_200_000;
        var header = Header(Body, ts, Secret);

        var ok = SignatureVerifier.Verify(Body, header, Secret, now: () => ts - 301);

        Assert.False(ok);
    }

    [Fact]
    public void Verify_ExactlyAtToleranceBoundary_ReturnsTrue()
    {
        const long ts = 1_716_200_000;
        var header = Header(Body, ts, Secret);

        var ok = SignatureVerifier.Verify(Body, header, Secret, toleranceSeconds: 300, now: () => ts + 300);

        Assert.True(ok);
    }

    [Fact]
    public void Verify_ToleranceZero_SkipsTimestampCheck()
    {
        const long ts = 1_716_200_000;
        var header = Header(Body, ts, Secret);

        // Way outside any window, but tolerance 0 disables the check.
        var ok = SignatureVerifier.Verify(Body, header, Secret, toleranceSeconds: 0, now: () => ts + 999_999);

        Assert.True(ok);
    }

    [Fact]
    public void Verify_CasingVariations_Accepted()
    {
        const long ts = 1_716_200_000;
        var v1 = Sign(Body, ts, Secret);
        var header = $"T={ts.ToString(CultureInfo.InvariantCulture)},V1={v1}";

        var ok = SignatureVerifier.Verify(Body, header, Secret, now: () => ts);

        Assert.True(ok);
    }

    [Fact]
    public void Verify_UppercaseHexSignature_Accepted()
    {
        const long ts = 1_716_200_000;
        var v1 = Sign(Body, ts, Secret).ToUpperInvariant();
        var header = $"t={ts.ToString(CultureInfo.InvariantCulture)},v1={v1}";

        var ok = SignatureVerifier.Verify(Body, header, Secret, now: () => ts);

        Assert.True(ok);
    }

    [Fact]
    public void Verify_EmptySecret_ReturnsFalse()
    {
        const long ts = 1_716_200_000;
        var header = Header(Body, ts, Secret);

        Assert.False(SignatureVerifier.Verify(Body, header, string.Empty, now: () => ts));
    }

    [Fact]
    public void Verify_EmptyHeader_ReturnsFalse()
    {
        Assert.False(SignatureVerifier.Verify(Body, string.Empty, Secret));
    }

    [Fact]
    public void Verify_NullBody_ReturnsFalse()
    {
        const long ts = 1_716_200_000;
        var header = Header(Body, ts, Secret);

        Assert.False(SignatureVerifier.Verify(null!, header, Secret, now: () => ts));
    }

    [Theory]
    [InlineData("garbage")]
    [InlineData("t=,v1=abc")]
    [InlineData("v1=abcdef")] // missing t
    [InlineData("t=1716200000")] // missing v1
    [InlineData("t=notanumber,v1=abcdef")] // non-numeric t
    [InlineData("t=-5,v1=abcdef")] // non-positive t
    [InlineData("t=0,v1=abcdef")] // zero t
    [InlineData("t=1716200000,v1=zzzz")] // non-hex v1
    [InlineData("t=1716200000,v1=abc")] // odd-length v1
    [InlineData("=novalue")] // malformed key
    public void Verify_MalformedHeaders_ReturnFalse(string header)
    {
        var ok = SignatureVerifier.Verify(Body, header, Secret, now: () => 1_716_200_000);

        Assert.False(ok);
    }

    [Fact]
    public void Verify_HmacLengthMismatch_ReturnsFalse()
    {
        const long ts = 1_716_200_000;
        // A well-formed but wrong-length hex string (2 bytes instead of 32).
        var header = $"t={ts.ToString(CultureInfo.InvariantCulture)},v1=abcd";

        var ok = SignatureVerifier.Verify(Body, header, Secret, now: () => ts);

        Assert.False(ok);
    }

    [Fact]
    public void Verify_WhitespacePaddedSegments_Accepted()
    {
        const long ts = 1_716_200_000;
        var v1 = Sign(Body, ts, Secret);
        var header = $" t = {ts.ToString(CultureInfo.InvariantCulture)} , v1 = {v1} ";

        var ok = SignatureVerifier.Verify(Body, header, Secret, now: () => ts);

        Assert.True(ok);
    }

    [Fact]
    public void DefaultToleranceSeconds_IsThreeHundred()
    {
        Assert.Equal(300, SignatureVerifier.DefaultToleranceSeconds);
    }
}
