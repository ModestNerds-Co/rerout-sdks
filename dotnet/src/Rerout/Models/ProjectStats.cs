using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Rerout.Models;

/// <summary>Aggregate analytics for a project across the requested window.</summary>
public sealed record ProjectStats
{
    /// <summary>Window size in days the totals span.</summary>
    [JsonPropertyName("days")]
    public required int Days { get; init; }

    /// <summary>Total clicks recorded in the window.</summary>
    [JsonPropertyName("total_clicks")]
    public required long TotalClicks { get; init; }

    /// <summary>Total QR scans (subset of <see cref="TotalClicks"/>) in the window.</summary>
    [JsonPropertyName("qr_scans")]
    public required long QrScans { get; init; }

    /// <summary>One point per day across the window. Gap-filled by the server.</summary>
    [JsonPropertyName("daily")]
    public IReadOnlyList<DailyClicksPoint> Daily { get; init; } = [];

    /// <summary>Top countries by click count.</summary>
    [JsonPropertyName("countries")]
    public IReadOnlyList<StatsBreakdown> Countries { get; init; } = [];

    /// <summary>Top referrers by click count.</summary>
    [JsonPropertyName("referrers")]
    public IReadOnlyList<StatsBreakdown> Referrers { get; init; } = [];

    /// <summary>Click share by device class.</summary>
    [JsonPropertyName("devices")]
    public IReadOnlyList<StatsBreakdown> Devices { get; init; } = [];

    /// <summary>Click share by browser or in-app web view.</summary>
    [JsonPropertyName("browsers")]
    public IReadOnlyList<StatsBreakdown> Browsers { get; init; } = [];

    /// <summary>Top short codes by click count.</summary>
    [JsonPropertyName("top_codes")]
    public IReadOnlyList<StatsBreakdown> TopCodes { get; init; } = [];
}

/// <summary>Information about the project that owns the current API key.</summary>
public sealed record ProjectInfo
{
    /// <summary>Project identifier.</summary>
    [JsonPropertyName("id")]
    public required string Id { get; init; }

    /// <summary>Human-readable project name.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; init; }

    /// <summary>URL-safe project slug.</summary>
    [JsonPropertyName("slug")]
    public required string Slug { get; init; }
}

/// <summary>A page of links returned by <see cref="Resources.Links.ListAsync"/>.</summary>
public sealed record ListLinksResult
{
    /// <summary>The links on this page.</summary>
    [JsonPropertyName("links")]
    public IReadOnlyList<Link> Links { get; init; } = [];

    /// <summary>
    /// Pass to the next <see cref="Resources.Links.ListAsync"/> call to fetch the
    /// next page. <c>null</c> when there are no more pages.
    /// </summary>
    [JsonPropertyName("next_cursor")]
    public long? NextCursor { get; init; }
}

/// <summary>Result of a delete call.</summary>
public sealed record DeleteResult
{
    /// <summary><c>true</c> when the link was deleted.</summary>
    [JsonPropertyName("deleted")]
    public required bool Deleted { get; init; }
}
