using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Rerout.Models;

/// <summary>
/// A label attached to a <see cref="Link"/>, as returned by the Rerout API on
/// create / update / get. Carries no link count — see <see cref="TagSummary"/>
/// for the list shape.
/// </summary>
public sealed record Tag
{
    /// <summary>Stable identifier for the tag (<c>tag_…</c>).</summary>
    [JsonPropertyName("id")]
    public required string Id { get; init; }

    /// <summary>Human-readable tag name.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; init; }

    /// <summary>Display color for the tag.</summary>
    [JsonPropertyName("color")]
    public required string Color { get; init; }
}

/// <summary>
/// A tag with the number of live (non-deleted) links it is attached to. This is
/// the shape returned by <see cref="Resources.Tags.ListAsync"/>; create / update
/// responses omit <see cref="LinkCount"/> and use plain <see cref="Tag"/>.
/// </summary>
public sealed record TagSummary
{
    /// <summary>Stable identifier for the tag (<c>tag_…</c>).</summary>
    [JsonPropertyName("id")]
    public required string Id { get; init; }

    /// <summary>Human-readable tag name.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; init; }

    /// <summary>Display color for the tag.</summary>
    [JsonPropertyName("color")]
    public required string Color { get; init; }

    /// <summary>Number of live (non-deleted) links this tag is attached to.</summary>
    [JsonPropertyName("link_count")]
    public required int LinkCount { get; init; }
}

/// <summary>
/// Result of <see cref="Resources.Tags.ListAsync"/> — the project's tags with
/// their live link counts.
/// </summary>
public sealed record ListTagsResult
{
    /// <summary>The tags registered to the project, each with its link count.</summary>
    [JsonPropertyName("tags")]
    public IReadOnlyList<TagSummary> Tags { get; init; } = [];
}

/// <summary>Request body for the <c>POST /v1/projects/me/tags</c> endpoint.</summary>
public sealed record CreateTagInput
{
    /// <summary>Tag label. Required.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; init; }

    /// <summary>
    /// Tag color. Optional; the server validates it against its palette and
    /// defaults to <c>teal</c> when omitted.
    /// </summary>
    [JsonPropertyName("color")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Color { get; init; }
}

/// <summary>
/// Patch for a tag (<c>PATCH /v1/projects/me/tags/:tag_id</c>). Both fields are
/// optional; only the fields that are set are sent, and omitted fields are left
/// unchanged on the server.
/// </summary>
public sealed record UpdateTagInput
{
    /// <summary>New tag name. Omit to leave it unchanged.</summary>
    [JsonPropertyName("name")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Name { get; init; }

    /// <summary>New tag color. Omit to leave it unchanged.</summary>
    [JsonPropertyName("color")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Color { get; init; }

    /// <summary>
    /// <c>true</c> when neither <see cref="Name"/> nor <see cref="Color"/> is
    /// set, so there is nothing to send.
    /// </summary>
    [JsonIgnore]
    internal bool IsEmpty => Name is null && Color is null;
}
