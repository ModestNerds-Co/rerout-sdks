using System.Text.Json.Serialization;

namespace Rerout.AspNetCore.Events;

/// <summary>
/// Payload of a <c>domain.failed</c> webhook — fired when a custom domain's
/// verification or TLS provisioning fails.
/// </summary>
public sealed record DomainFailed
{
    /// <summary>The custom domain hostname that failed.</summary>
    [JsonPropertyName("hostname")]
    public required string Hostname { get; init; }

    /// <summary>Stable failure reason code (e.g. <c>dns_unverified</c>, <c>tls_failed</c>).</summary>
    [JsonPropertyName("reason")]
    public required string Reason { get; init; }

    /// <summary>Human-readable description of the failure.</summary>
    [JsonPropertyName("message")]
    public string? Message { get; init; }

    /// <summary>Unix seconds — when the failure was recorded.</summary>
    [JsonPropertyName("failed_at")]
    public required long FailedAt { get; init; }
}
