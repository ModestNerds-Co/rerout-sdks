using System.Collections.Generic;

namespace Rerout.Models;

/// <summary>
/// Request body for the <c>PATCH /v1/links/:code</c> endpoint. Every field is
/// optional. Use <see cref="Optional{T}.Set"/> with a nullable type to clear a
/// field on the server (e.g.
/// <c>SeoTitle = Optional&lt;string?&gt;.Set(null)</c>).
/// </summary>
public sealed record UpdateLinkInput
{
    /// <summary>New destination URL.</summary>
    public Optional<string> TargetUrl { get; init; }

    /// <summary>
    /// New expiry (unix seconds). Send <see cref="Optional{T}.Set"/> with
    /// <c>null</c> to clear an existing expiry.
    /// </summary>
    public Optional<long?> ExpiresAt { get; init; }

    /// <summary>Activate or deactivate the link.</summary>
    public Optional<bool> IsActive { get; init; }

    /// <summary>New preview title. <c>Set(null)</c> clears.</summary>
    public Optional<string?> SeoTitle { get; init; }

    /// <summary>New preview description. <c>Set(null)</c> clears.</summary>
    public Optional<string?> SeoDescription { get; init; }

    /// <summary>New preview image URL. <c>Set(null)</c> clears.</summary>
    public Optional<string?> SeoImageUrl { get; init; }

    /// <summary>New canonical URL. <c>Set(null)</c> clears.</summary>
    public Optional<string?> SeoCanonicalUrl { get; init; }

    /// <summary>Toggle whether the preview page is noindex.</summary>
    public Optional<bool> SeoNoindex { get; init; }

    /// <summary>
    /// Smart Links — set a password, or <c>Set(null)</c> to clear it.
    /// </summary>
    public Optional<string?> Password { get; init; }

    /// <summary>
    /// Smart Links — set a click cap, or <c>Set(null)</c> to clear it.
    /// </summary>
    public Optional<long?> MaxClicks { get; init; }

    /// <summary>Smart Links — toggle conversion tracking.</summary>
    public Optional<bool> TrackConversions { get; init; }

    /// <summary>
    /// Smart Links — full-replace the routing rules. An empty list clears them.
    /// </summary>
    public Optional<IReadOnlyList<RoutingRule>> RoutingRules { get; init; }

    /// <summary>
    /// Smart Links — full-replace the A/B variants. An empty list clears them.
    /// </summary>
    public Optional<IReadOnlyList<AbVariantInput>> AbVariants { get; init; }

    /// <summary>
    /// Materialize this input as the JSON object the API expects. Properties
    /// whose <see cref="Optional{T}"/> is <see cref="Optional{T}.Unset"/> are
    /// omitted entirely.
    /// </summary>
    public IDictionary<string, object?> ToPayload()
    {
        var payload = new Dictionary<string, object?>();
        if (TargetUrl.HasValue) payload["target_url"] = TargetUrl.Value;
        if (ExpiresAt.HasValue) payload["expires_at"] = ExpiresAt.Value;
        if (IsActive.HasValue) payload["is_active"] = IsActive.Value;
        if (SeoTitle.HasValue) payload["seo_title"] = SeoTitle.Value;
        if (SeoDescription.HasValue) payload["seo_description"] = SeoDescription.Value;
        if (SeoImageUrl.HasValue) payload["seo_image_url"] = SeoImageUrl.Value;
        if (SeoCanonicalUrl.HasValue) payload["seo_canonical_url"] = SeoCanonicalUrl.Value;
        if (SeoNoindex.HasValue) payload["seo_noindex"] = SeoNoindex.Value;
        if (Password.HasValue) payload["password"] = Password.Value;
        if (MaxClicks.HasValue) payload["max_clicks"] = MaxClicks.Value;
        if (TrackConversions.HasValue) payload["track_conversions"] = TrackConversions.Value;
        if (RoutingRules.HasValue) payload["routing_rules"] = RoutingRules.Value;
        if (AbVariants.HasValue) payload["ab_variants"] = AbVariants.Value;
        return payload;
    }

    /// <summary><c>true</c> when no field is set — the API would reject this as a no-op.</summary>
    public bool IsEmpty =>
        !TargetUrl.HasValue
        && !ExpiresAt.HasValue
        && !IsActive.HasValue
        && !SeoTitle.HasValue
        && !SeoDescription.HasValue
        && !SeoImageUrl.HasValue
        && !SeoCanonicalUrl.HasValue
        && !SeoNoindex.HasValue
        && !Password.HasValue
        && !MaxClicks.HasValue
        && !TrackConversions.HasValue
        && !RoutingRules.HasValue
        && !AbVariants.HasValue;
}
