using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Rerout.Models;

/// <summary>Click-count breakdown by dimension (country, referrer, etc.).</summary>
public sealed record StatsBreakdown
{
    /// <summary>The bucket — country code, referrer host, device class, etc.</summary>
    [JsonPropertyName("value")]
    public required string Value { get; init; }

    /// <summary>Clicks attributed to this bucket.</summary>
    [JsonPropertyName("clicks")]
    public required long Clicks { get; init; }
}

/// <summary>One day's click + scan totals.</summary>
public sealed record DailyClicksPoint
{
    /// <summary>Unix seconds — start of the day (UTC).</summary>
    [JsonPropertyName("day")]
    public required long Day { get; init; }

    /// <summary>Total clicks recorded on this day.</summary>
    [JsonPropertyName("clicks")]
    public required long Clicks { get; init; }

    /// <summary>Subset of <see cref="Clicks"/> attributed to a QR scan.</summary>
    [JsonPropertyName("qr_scans")]
    public required long QrScans { get; init; }
}

/// <summary>Analytics for a single short link across the requested window.</summary>
public sealed record LinkStats
{
    /// <summary>The short code these stats belong to.</summary>
    [JsonPropertyName("code")]
    public required string Code { get; init; }

    /// <summary>Window size in days the totals span.</summary>
    [JsonPropertyName("days")]
    public required int Days { get; init; }

    /// <summary>Total clicks in the window.</summary>
    [JsonPropertyName("total_clicks")]
    public required long TotalClicks { get; init; }

    /// <summary>Subset of <see cref="TotalClicks"/> attributed to QR scans.</summary>
    [JsonPropertyName("qr_scans")]
    public required long QrScans { get; init; }

    /// <summary>Top countries.</summary>
    [JsonPropertyName("countries")]
    public IReadOnlyList<StatsBreakdown> Countries { get; init; } = [];

    /// <summary>Top referrers.</summary>
    [JsonPropertyName("referrers")]
    public IReadOnlyList<StatsBreakdown> Referrers { get; init; } = [];
}
