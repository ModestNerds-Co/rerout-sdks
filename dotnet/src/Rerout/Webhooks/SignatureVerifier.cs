using System;
using System.Globalization;
using System.Security.Cryptography;
using System.Text;

namespace Rerout.Webhooks;

/// <summary>
/// Verifies inbound <c>X-Rerout-Signature</c> webhook headers.
/// </summary>
/// <remarks>
/// <para>
/// Rerout signs every webhook delivery as
/// <c>t={unix_seconds},v1={hex_hmac_sha256}</c>, where the HMAC is computed over
/// <c>"{timestamp}.{raw_body}"</c> using the endpoint signing secret
/// (<c>whsec_…</c>) as the key.
/// </para>
/// <para>
/// Always verify against the <em>raw</em>, unmodified request body — any
/// re-serialization changes the bytes and invalidates the signature.
/// </para>
/// </remarks>
/// <example>
/// <code>
/// var ok = SignatureVerifier.Verify(
///     rawBody,
///     request.Headers["X-Rerout-Signature"],
///     Environment.GetEnvironmentVariable("REROUT_WEBHOOK_SECRET")!);
/// if (!ok)
/// {
///     return Results.Unauthorized();
/// }
/// </code>
/// </example>
public static class SignatureVerifier
{
    /// <summary>
    /// Default tolerance window in seconds between the <c>t=</c> timestamp and
    /// the current time — five minutes. Protects against captured-replay
    /// attacks.
    /// </summary>
    public const int DefaultToleranceSeconds = 300;

    /// <summary>
    /// Verify a Rerout webhook signature.
    /// </summary>
    /// <param name="rawBody">The raw, unmodified request body.</param>
    /// <param name="signatureHeader">The value of the <c>X-Rerout-Signature</c> header.</param>
    /// <param name="secret">The endpoint signing secret (<c>whsec_…</c>).</param>
    /// <param name="toleranceSeconds">
    /// Window in seconds. Defaults to <see cref="DefaultToleranceSeconds"/>. Pass
    /// <c>0</c> to disable the timestamp staleness check entirely.
    /// </param>
    /// <param name="now">
    /// Injectable clock returning the current time in unix seconds — for
    /// deterministic tests. Defaults to the system clock.
    /// </param>
    /// <returns>
    /// <c>true</c> only when the header parses cleanly, the timestamp is within
    /// tolerance, and the computed HMAC matches the supplied <c>v1</c> in
    /// constant time. <c>false</c> for every failure mode.
    /// </returns>
    public static bool Verify(
        string rawBody,
        string signatureHeader,
        string secret,
        int toleranceSeconds = DefaultToleranceSeconds,
        Func<long>? now = null)
    {
        if (rawBody is null
            || string.IsNullOrEmpty(signatureHeader)
            || string.IsNullOrEmpty(secret))
        {
            return false;
        }

        if (!TryParseHeader(signatureHeader, out var timestamp, out var v1Hex))
        {
            return false;
        }

        if (toleranceSeconds > 0)
        {
            var nowSeconds = now?.Invoke() ?? DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            if (Math.Abs(nowSeconds - timestamp) > toleranceSeconds)
            {
                return false;
            }
        }

        if (!TryDecodeHex(v1Hex, out var provided))
        {
            return false;
        }

        var signedPayload = Encoding.UTF8.GetBytes(
            string.Concat(timestamp.ToString(CultureInfo.InvariantCulture), ".", rawBody));
        var expected = HMACSHA256.HashData(Encoding.UTF8.GetBytes(secret), signedPayload);

        if (provided.Length != expected.Length)
        {
            return false;
        }

        return CryptographicOperations.FixedTimeEquals(provided, expected);
    }

    private static bool TryParseHeader(string header, out long timestamp, out string v1Hex)
    {
        timestamp = 0;
        v1Hex = string.Empty;
        var haveTimestamp = false;
        var haveV1 = false;

        foreach (var rawSegment in header.Split(','))
        {
            var segment = rawSegment;
            var eq = segment.IndexOf('=', StringComparison.Ordinal);
            if (eq <= 0)
            {
                continue;
            }

            var key = segment[..eq].Trim().ToLowerInvariant();
            var value = segment[(eq + 1)..].Trim();

            if (key == "t")
            {
                if (long.TryParse(value, NumberStyles.None, CultureInfo.InvariantCulture, out var parsed)
                    && parsed > 0)
                {
                    timestamp = parsed;
                    haveTimestamp = true;
                }
            }
            else if (key == "v1")
            {
                if (value.Length > 0)
                {
                    v1Hex = value;
                    haveV1 = true;
                }
            }
        }

        return haveTimestamp && haveV1;
    }

    private static bool TryDecodeHex(string hex, out byte[] bytes)
    {
        bytes = Array.Empty<byte>();
        if (hex.Length == 0 || (hex.Length & 1) == 1)
        {
            return false;
        }

        var output = new byte[hex.Length / 2];
        for (var i = 0; i < output.Length; i++)
        {
            var hi = HexDigit(hex[i * 2]);
            var lo = HexDigit(hex[(i * 2) + 1]);
            if (hi < 0 || lo < 0)
            {
                return false;
            }

            output[i] = (byte)((hi << 4) | lo);
        }

        bytes = output;
        return true;
    }

    private static int HexDigit(char c) => c switch
    {
        >= '0' and <= '9' => c - '0',
        >= 'a' and <= 'f' => c - 'a' + 10,
        >= 'A' and <= 'F' => c - 'A' + 10,
        _ => -1,
    };
}
