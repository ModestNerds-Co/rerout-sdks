using System.Text.Json.Serialization;

namespace Rerout.AspNetCore.Events;

/// <summary>
/// Payload of a <c>link.clicked</c> webhook — fired each time one of the
/// project's short links resolves a redirect.
/// </summary>
public sealed record LinkClicked
{
    /// <summary>The short code that was clicked.</summary>
    [JsonPropertyName("code")]
    public required string Code { get; init; }

    /// <summary>Destination URL the click resolved to.</summary>
    [JsonPropertyName("target_url")]
    public required string TargetUrl { get; init; }

    /// <summary>Verified custom domain hosting the link, when one is bound.</summary>
    [JsonPropertyName("domain_hostname")]
    public string? DomainHostname { get; init; }

    /// <summary>Two-letter country code resolved from the visitor's IP, when known.</summary>
    [JsonPropertyName("country")]
    public string? Country { get; init; }

    /// <summary>Referrer host, when the visitor's browser supplied one.</summary>
    [JsonPropertyName("referrer")]
    public string? Referrer { get; init; }

    /// <summary>Whether the click originated from a scanned QR code.</summary>
    [JsonPropertyName("is_qr")]
    public bool IsQr { get; init; }

    /// <summary>Unix seconds — when the click was recorded.</summary>
    [JsonPropertyName("clicked_at")]
    public required long ClickedAt { get; init; }
}
