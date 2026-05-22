using System.Text.Json.Serialization;

namespace Rerout.AspNetCore.Events;

/// <summary>
/// Payload of a <c>qr.scanned</c> webhook — fired when a rendered QR code for
/// one of the project's links is scanned.
/// </summary>
public sealed record QrScanned
{
    /// <summary>The short code encoded in the scanned QR.</summary>
    [JsonPropertyName("code")]
    public required string Code { get; init; }

    /// <summary>Destination URL the scan resolved to.</summary>
    [JsonPropertyName("target_url")]
    public required string TargetUrl { get; init; }

    /// <summary>Verified custom domain encoded in the QR, when one is bound.</summary>
    [JsonPropertyName("domain_hostname")]
    public string? DomainHostname { get; init; }

    /// <summary>Two-letter country code resolved from the scanner's IP, when known.</summary>
    [JsonPropertyName("country")]
    public string? Country { get; init; }

    /// <summary>Unix seconds — when the scan was recorded.</summary>
    [JsonPropertyName("scanned_at")]
    public required long ScannedAt { get; init; }
}
