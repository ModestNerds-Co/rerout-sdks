using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Rerout.Models;

/// <summary>A single link to create in a batch via <c>POST /v1/links/batch</c>.</summary>
public sealed record BatchLinkInput
{
    /// <summary>Absolute <c>https://</c> destination URL.</summary>
    [JsonPropertyName("target_url")]
    public required string TargetUrl { get; init; }

    /// <summary>Custom path. Only valid with a verified <see cref="DomainHostname"/>.</summary>
    [JsonPropertyName("code")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Code { get; init; }

    /// <summary>Unix seconds — expiration. Omit for a permanent link.</summary>
    [JsonPropertyName("expires_at")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public long? ExpiresAt { get; init; }

    /// <summary>Verified custom domain to host this link on.</summary>
    [JsonPropertyName("domain_hostname")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? DomainHostname { get; init; }
}

/// <summary>Per-item outcome of a batch create.</summary>
public sealed record BatchLinkResult
{
    /// <summary>Index of the input item this result corresponds to.</summary>
    [JsonPropertyName("index")]
    public int Index { get; init; }

    /// <summary>Whether the item was created.</summary>
    [JsonPropertyName("ok")]
    public bool Ok { get; init; }

    /// <summary>Allocated code, when <see cref="Ok"/>.</summary>
    [JsonPropertyName("code")]
    public string? Code { get; init; }

    /// <summary>Failure reason, when not <see cref="Ok"/>.</summary>
    [JsonPropertyName("error")]
    public string? Error { get; init; }
}

/// <summary>Result of a batch link create (partial-success).</summary>
public sealed record BatchCreateLinksResult
{
    /// <summary>Number of links successfully created.</summary>
    [JsonPropertyName("created")]
    public int Created { get; init; }

    /// <summary>Total number of items in the batch.</summary>
    [JsonPropertyName("total")]
    public int Total { get; init; }

    /// <summary>Per-item outcomes, in input order.</summary>
    [JsonPropertyName("results")]
    public IReadOnlyList<BatchLinkResult> Results { get; init; } = [];
}
