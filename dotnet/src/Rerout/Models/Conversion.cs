using System.Text.Json.Serialization;

namespace Rerout.Models;

/// <summary>
/// Request body for the <c>POST /v1/conversions</c> endpoint. Idempotent per
/// <c>(ClickId, EventName)</c>.
/// </summary>
public sealed record RecordConversionInput
{
    /// <summary>The click id (<c>rrid</c>) minted on the tracked redirect.</summary>
    [JsonPropertyName("click_id")]
    public required string ClickId { get; init; }

    /// <summary>Conversion event label (e.g. <c>purchase</c>, <c>signup</c>).</summary>
    [JsonPropertyName("event_name")]
    public required string EventName { get; init; }

    /// <summary>Optional monetary value in minor units (cents).</summary>
    [JsonPropertyName("value_cents")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public long? ValueCents { get; init; }

    /// <summary>Optional ISO 4217 currency code (e.g. <c>USD</c>).</summary>
    [JsonPropertyName("currency")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Currency { get; init; }
}

/// <summary>Result of recording a conversion.</summary>
public sealed record RecordedConversion
{
    /// <summary>Whether the conversion is now recorded.</summary>
    [JsonPropertyName("recorded")]
    public bool Recorded { get; init; }

    /// <summary>
    /// Whether this <c>(click_id, event_name)</c> was already recorded
    /// (idempotent).
    /// </summary>
    [JsonPropertyName("duplicate")]
    public bool Duplicate { get; init; }
}
