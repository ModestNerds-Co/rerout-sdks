using System.Text.Json.Serialization;

namespace Rerout.Models;

/// <summary>Request body for the <c>POST /v1/links</c> endpoint.</summary>
public sealed record CreateLinkInput
{
    /// <summary>Absolute <c>https://</c> destination URL. Max 2048 characters.</summary>
    [JsonPropertyName("target_url")]
    public required string TargetUrl { get; init; }

    /// <summary>Verified custom domain to host this link on. Omit for <c>rerout.co/:code</c>.</summary>
    [JsonPropertyName("domain_hostname")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? DomainHostname { get; init; }

    /// <summary>Custom path. Only valid when <see cref="DomainHostname"/> is supplied.</summary>
    [JsonPropertyName("code")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Code { get; init; }

    /// <summary>Unix seconds — expiration. Omit for a permanent link.</summary>
    [JsonPropertyName("expires_at")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public long? ExpiresAt { get; init; }

    /// <summary>Override social preview title. Max 90 characters.</summary>
    [JsonPropertyName("seo_title")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? SeoTitle { get; init; }

    /// <summary>Override social preview description. Max 220 characters.</summary>
    [JsonPropertyName("seo_description")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? SeoDescription { get; init; }

    /// <summary>Absolute <c>https://</c> social preview image URL.</summary>
    [JsonPropertyName("seo_image_url")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? SeoImageUrl { get; init; }

    /// <summary>Canonical URL for the preview HTML.</summary>
    [JsonPropertyName("seo_canonical_url")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? SeoCanonicalUrl { get; init; }

    /// <summary>Whether the preview page should be marked <c>noindex</c>.</summary>
    [JsonPropertyName("seo_noindex")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public bool? SeoNoindex { get; init; }
}
