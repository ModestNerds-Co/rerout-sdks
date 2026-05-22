using System;
using System.Globalization;
using System.Security.Cryptography;
using System.Text;

namespace Rerout.AspNetCore.Tests;

/// <summary>Builds <c>X-Rerout-Signature</c> headers for test requests.</summary>
internal static class WebhookSigning
{
    /// <summary>Compute the lowercase hex HMAC-SHA256 over <c>"{ts}.{body}"</c>.</summary>
    public static string Sign(string body, long timestamp, string secret)
    {
        var payload = Encoding.UTF8.GetBytes($"{timestamp.ToString(CultureInfo.InvariantCulture)}.{body}");
        var hash = HMACSHA256.HashData(Encoding.UTF8.GetBytes(secret), payload);
        return Convert.ToHexString(hash).ToLowerInvariant();
    }

    /// <summary>Build the full <c>t=…,v1=…</c> signature header.</summary>
    public static string Header(string body, string secret, long? timestamp = null)
    {
        var ts = timestamp ?? DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        return $"t={ts.ToString(CultureInfo.InvariantCulture)},v1={Sign(body, ts, secret)}";
    }
}
