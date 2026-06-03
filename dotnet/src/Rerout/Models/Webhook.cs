using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Rerout.Models;

/// <summary>
/// A webhook endpoint registered to the project. Mirrors the server-side
/// <c>WebhookEndpointResponse</c> shape so JSON parses without transformation.
/// </summary>
public sealed record Webhook
{
    /// <summary>Endpoint identifier (<c>wh_…</c>).</summary>
    [JsonPropertyName("id")]
    public required string Id { get; init; }

    /// <summary>Project that owns the endpoint.</summary>
    [JsonPropertyName("project_id")]
    public required string ProjectId { get; init; }

    /// <summary>Human-readable label for the endpoint.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; init; }

    /// <summary>Public <c>https://</c> URL that receives signed POST deliveries.</summary>
    [JsonPropertyName("url")]
    public required string Url { get; init; }

    /// <summary>Event types this endpoint is subscribed to.</summary>
    [JsonPropertyName("events")]
    public IReadOnlyList<string> Events { get; init; } = [];

    /// <summary>Whether the endpoint is currently active.</summary>
    [JsonPropertyName("is_active")]
    public required bool IsActive { get; init; }

    /// <summary>Delivery payload encoding — <c>json</c> or <c>slack</c>.</summary>
    [JsonPropertyName("payload_format")]
    public required string PayloadFormat { get; init; }

    /// <summary>Unix seconds — endpoint creation time.</summary>
    [JsonPropertyName("created_at")]
    public required long CreatedAt { get; init; }

    /// <summary>Unix seconds — last mutation.</summary>
    [JsonPropertyName("updated_at")]
    public required long UpdatedAt { get; init; }

    /// <summary>Unix seconds — last delivery attempt. <c>null</c> until one occurs.</summary>
    [JsonPropertyName("last_delivery_at")]
    public long? LastDeliveryAt { get; init; }

    /// <summary>Unix seconds — last successful delivery. <c>null</c> until one occurs.</summary>
    [JsonPropertyName("last_success_at")]
    public long? LastSuccessAt { get; init; }

    /// <summary>Unix seconds — last failed delivery. <c>null</c> until one occurs.</summary>
    [JsonPropertyName("last_failure_at")]
    public long? LastFailureAt { get; init; }
}

/// <summary>Request body for the <c>POST /v1/projects/me/webhooks</c> endpoint.</summary>
public sealed record CreateWebhookInput
{
    /// <summary>Human-readable label for the endpoint. Required.</summary>
    [JsonPropertyName("name")]
    public required string Name { get; init; }

    /// <summary>Public <c>https://</c> URL that receives signed POST deliveries. Required.</summary>
    [JsonPropertyName("url")]
    public required string Url { get; init; }

    /// <summary>
    /// Event types to subscribe to (e.g. <c>link.created</c>). At least one is
    /// required.
    /// </summary>
    [JsonPropertyName("events")]
    public required IReadOnlyList<string> Events { get; init; }

    /// <summary>Whether the endpoint starts active. Omit to default to <c>true</c>.</summary>
    [JsonPropertyName("is_active")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public bool? IsActive { get; init; }

    /// <summary>
    /// Payload encoding (<c>json</c> or <c>slack</c>). Omit to let the server
    /// default (<c>json</c>, or <c>slack</c> for Slack URLs).
    /// </summary>
    [JsonPropertyName("payload_format")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? PayloadFormat { get; init; }
}

/// <summary>
/// Result of creating a webhook endpoint. The <see cref="SigningSecret"/>
/// (<c>whsec_…</c>) is returned <b>once</b> — store it now to verify deliveries;
/// it is never shown again.
/// </summary>
public sealed record CreatedWebhook
{
    /// <summary>The endpoint that was created.</summary>
    [JsonPropertyName("endpoint")]
    public required Webhook Endpoint { get; init; }

    /// <summary>Signing secret (<c>whsec_…</c>) — returned only at creation time.</summary>
    [JsonPropertyName("signing_secret")]
    public required string SigningSecret { get; init; }
}

/// <summary>
/// Result of listing webhook endpoints, returned by
/// <see cref="Resources.Webhooks.ListAsync"/>.
/// </summary>
public sealed record ListWebhooksResult
{
    /// <summary>The webhook endpoints registered to the project.</summary>
    [JsonPropertyName("endpoints")]
    public IReadOnlyList<Webhook> Endpoints { get; init; } = [];

    /// <summary>Every event type the server can deliver.</summary>
    [JsonPropertyName("event_types")]
    public IReadOnlyList<string> EventTypes { get; init; } = [];
}
