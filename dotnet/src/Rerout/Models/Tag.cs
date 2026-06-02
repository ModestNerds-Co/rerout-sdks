using System.Text.Json.Serialization;

namespace Rerout.Models;

/// <summary>A label attached to a <see cref="Link"/>, as returned by the Rerout API.</summary>
public sealed record Tag
{
    /// <summary>Stable identifier for the tag.</summary>
    [JsonPropertyName("id")]
    public required string Id { get; init; }

    /// <summary>Human-readable tag name.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; init; }

    /// <summary>Display color for the tag.</summary>
    [JsonPropertyName("color")]
    public required string Color { get; init; }
}
