using System;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Rerout.Internal;
using Rerout.Models;

namespace Rerout.Resources;

/// <summary>
/// Webhook endpoint management: create, list, delete. Reached through
/// <see cref="ReroutClient.Webhooks"/>. The project is resolved from the API
/// key — there is no project id in the path.
/// </summary>
/// <remarks>
/// This manages webhook <i>endpoints</i>. To verify the signature of an inbound
/// webhook delivery, use <see cref="Rerout.Webhooks.SignatureVerifier"/>.
/// </remarks>
public sealed class Webhooks
{
    private readonly ReroutHttpHandler _http;

    internal Webhooks(ReroutHttpHandler http) => _http = http;

    /// <summary>
    /// Create a webhook endpoint for the project that owns the API key. The
    /// returned <see cref="CreatedWebhook.SigningSecret"/> is shown once —
    /// persist it to verify deliveries.
    /// </summary>
    /// <param name="input">The endpoint to create. <c>Name</c>, <c>Url</c>, and a non-empty <c>Events</c> list are required.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>The created endpoint and its one-time signing secret.</returns>
    /// <exception cref="ReroutException">On any API or transport failure.</exception>
    public Task<CreatedWebhook> CreateAsync(CreateWebhookInput input, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<CreatedWebhook>(
            new ReroutRequest
            {
                Method = HttpMethod.Post,
                Path = "/v1/projects/me/webhooks",
                Body = input,
            },
            cancellationToken);
    }

    /// <summary>List webhook endpoints and the event types the server can deliver.</summary>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    public Task<ListWebhooksResult> ListAsync(CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<ListWebhooksResult>(
            new ReroutRequest
            {
                Method = HttpMethod.Get,
                Path = "/v1/projects/me/webhooks",
            },
            cancellationToken);
    }

    /// <summary>
    /// Soft-delete a webhook endpoint and abandon its pending deliveries.
    /// Idempotent.
    /// </summary>
    /// <param name="endpointId">The endpoint identifier (<c>wh_…</c>).</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    public Task<DeleteResult> DeleteAsync(string endpointId, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<DeleteResult>(
            new ReroutRequest
            {
                Method = HttpMethod.Delete,
                Path = $"/v1/projects/me/webhooks/{EncodeId(endpointId)}",
            },
            cancellationToken);
    }

    internal static string EncodeId(string endpointId) => Uri.EscapeDataString(endpointId);
}
