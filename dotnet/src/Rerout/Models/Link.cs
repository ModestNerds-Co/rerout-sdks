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

    /// <summary>Smart Links — whether a password is required to follow this link.</summary>
    [JsonPropertyName("password_protected")]
    public bool PasswordProtected { get; init; }

    /// <summary>Smart Links — click cap, or <c>null</c> when uncapped.</summary>
    [JsonPropertyName("max_clicks")]
    public long? MaxClicks { get; init; }

    /// <summary>Smart Links — total clicks recorded against this link.</summary>
    [JsonPropertyName("click_count")]
    public long ClickCount { get; init; }

    /// <summary>Smart Links — whether conversion tracking is enabled.</summary>
    [JsonPropertyName("track_conversions")]
    public bool TrackConversions { get; init; }

    /// <summary>Smart Links — ordered geo/device routing rules.</summary>
    [JsonPropertyName("routing_rules")]
    public IReadOnlyList<RoutingRule> RoutingRules { get; init; } = [];

    /// <summary>Smart Links — weighted A/B destinations.</summary>
    [JsonPropertyName("ab_variants")]
    public IReadOnlyList<AbVariant> AbVariants { get; init; } = [];
}

/// <summary>
/// A geo/device routing rule (Smart Links). When the condition matches the
/// incoming request, the redirect resolves to <see cref="TargetUrl"/> instead of
/// the link's default destination.
/// </summary>
/// <remarks>
/// <see cref="ConditionType"/> is <c>country</c> or <c>device</c>;
/// <see cref="ConditionOp"/> is <c>is</c>, <c>is_not</c>, or <c>in</c>. These are
/// sent verbatim and not validated client-side.
/// </remarks>
public sealed record RoutingRule
{
    /// <summary>What to match against — <c>country</c> or <c>device</c>.</summary>
    [JsonPropertyName("condition_type")]
    public required string ConditionType { get; init; }

    /// <summary>Comparison operator — <c>is</c>, <c>is_not</c>, or <c>in</c>.</summary>
    [JsonPropertyName("condition_op")]
    public required string ConditionOp { get; init; }

    /// <summary>Value(s) to compare against (e.g. <c>ZA</c>, <c>US,GB</c>, <c>mobile</c>).</summary>
    [JsonPropertyName("condition_value")]
    public required string ConditionValue { get; init; }

    /// <summary>Destination when the rule matches.</summary>
    [JsonPropertyName("target_url")]
    public required string TargetUrl { get; init; }
}

/// <summary>
/// A weighted A/B destination (Smart Links), as returned by the API. To supply
/// variants on create/update, use <see cref="AbVariantInput"/>.
/// </summary>
public sealed record AbVariant
{
    /// <summary>Stable variant id assigned by the server.</summary>
    [JsonPropertyName("id")]
    public required long Id { get; init; }

    /// <summary>Destination for this variant.</summary>
    [JsonPropertyName("target_url")]
    public required string TargetUrl { get; init; }

    /// <summary>Relative weight in the split.</summary>
    [JsonPropertyName("weight")]
    public required int Weight { get; init; }
}

/// <summary>
/// A weighted A/B destination as supplied on create/update. <see cref="Weight"/>
/// is optional and defaults server-side when omitted.
/// </summary>
public sealed record AbVariantInput
{
    /// <summary>Destination for this variant.</summary>
    [JsonPropertyName("target_url")]
    public required string TargetUrl { get; init; }

    /// <summary>Relative weight in the split. Omit to default server-side.</summary>
    [JsonPropertyName("weight")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public int? Weight { get; init; }
}
