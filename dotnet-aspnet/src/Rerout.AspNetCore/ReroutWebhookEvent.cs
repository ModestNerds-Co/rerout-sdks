using System;
using System.Text.Json;

namespace Rerout.AspNetCore;

/// <summary>
/// A verified, deserialized Rerout webhook delivery handed to
/// <see cref="IReroutEventHandler.HandleAsync"/>.
/// </summary>
/// <remarks>
/// The <see cref="Type"/> string identifies the event; <see cref="Data"/> holds
/// the raw payload object. Use <see cref="GetData{T}"/> to deserialize it into
/// one of the strongly-typed records in <c>Rerout.AspNetCore.Events</c>.
/// </remarks>
public sealed class ReroutWebhookEvent
{
    private readonly JsonElement _data;

    /// <summary>Construct an event from its parsed parts. Used by the middleware.</summary>
    public ReroutWebhookEvent(string type, string id, long createdAt, JsonElement data)
    {
        Type = type;
        Id = id;
        CreatedAt = createdAt;
        _data = data.Clone();
    }

    /// <summary>The event type, e.g. <c>link.clicked</c>, <c>qr.scanned</c>, <c>domain.failed</c>.</summary>
    public string Type { get; }

    /// <summary>Unique delivery identifier — useful for idempotency keys.</summary>
    public string Id { get; }

    /// <summary>Unix seconds — when Rerout generated the event.</summary>
    public long CreatedAt { get; }

    /// <summary>The raw event payload as a <see cref="JsonElement"/>.</summary>
    public JsonElement Data => _data;

    /// <summary>
    /// Deserialize <see cref="Data"/> into a strongly-typed event record.
    /// </summary>
    /// <typeparam name="T">One of the records in <c>Rerout.AspNetCore.Events</c>.</typeparam>
    /// <exception cref="InvalidOperationException">
    /// Thrown when the payload cannot be deserialized into <typeparamref name="T"/>.
    /// </exception>
    public T GetData<T>()
    {
        var value = _data.Deserialize<T>(ReroutWebhookJson.Options);
        if (value is null)
        {
            throw new InvalidOperationException(
                $"Webhook payload for '{Type}' could not be deserialized as {typeof(T).Name}.");
        }

        return value;
    }
}
