using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Rerout.Models;

/// <summary>A short link as returned by the Rerout API.</summary>
public sealed record Link
{
    /// <summary>The short link path code.</summary>
    [JsonPropertyName("code")]
    public required string Code { get; init; }

    /// <summary>Fully-qualified short URL — <c>https://{host}/{code}</c>.</summary>
    [JsonPropertyName("short_url")]
    public required string ShortUrl { get; init; }

    /// <summary>Verified custom domain hosting this link, when one is bound.</summary>
    [JsonPropertyName("domain_hostname")]
    public string? DomainHostname { get; init; }

    /// <summary>Destination the redirect resolves to.</summary>
    [JsonPropertyName("target_url")]
    public required string TargetUrl { get; init; }

    /// <summary>Project that owns the link.</summary>
    [JsonPropertyName("project_id")]
    public required string ProjectId { get; init; }

    /// <summary>Unix seconds — expiration. <c>null</c> for permanent links.</summary>
    [JsonPropertyName("expires_at")]
    public long? ExpiresAt { get; init; }

    /// <summary>Whether the link is currently active.</summary>
    [JsonPropertyName("is_active")]
    public required bool IsActive { get; init; }

    /// <summary>Override social preview title.</summary>
    [JsonPropertyName("seo_title")]
    public string? SeoTitle { get; init; }

    /// <summary>Override social preview description.</summary>
    [JsonPropertyName("seo_description")]
    public string? SeoDescription { get; init; }

    /// <summary>Override social preview image URL.</summary>
    [JsonPropertyName("seo_image_url")]
    public string? SeoImageUrl { get; init; }

    /// <summary>Override preview canonical URL.</summary>
    [JsonPropertyName("seo_canonical_url")]
    public string? SeoCanonicalUrl { get; init; }

    /// <summary>Whether the preview HTML should be indexed.</summary>
    [JsonPropertyName("seo_noindex")]
    public bool SeoNoindex { get; init; }

    /// <summary>Unix seconds — last SEO field change.</summary>
    [JsonPropertyName("seo_updated_at")]
    public long? SeoUpdatedAt { get; init; }

    /// <summary>Unix seconds — link creation time.</summary>
    [JsonPropertyName("created_at")]
    public required long CreatedAt { get; init; }

    /// <summary>Unix seconds — last mutation.</summary>
    [JsonPropertyName("updated_at")]
    public required long UpdatedAt { get; init; }

    /// <summary>Read-only tags attached to the link. Empty when none are set.</summary>
    [JsonPropertyName("tags")]
    public IReadOnlyList<Tag> Tags { get; init; } = [];
}
