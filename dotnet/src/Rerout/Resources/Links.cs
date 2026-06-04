using System;
using System.Collections.Generic;
using System.Globalization;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Rerout.Internal;
using Rerout.Models;

namespace Rerout.Resources;

/// <summary>
/// Link operations: create, list, get, update, delete, and per-link stats.
/// Reached through <see cref="ReroutClient.Links"/>.
/// </summary>
public sealed class Links
{
    private readonly ReroutHttpHandler _http;

    internal Links(ReroutHttpHandler http) => _http = http;

    /// <summary>Create a new short link.</summary>
    /// <param name="input">The link to create. <c>TargetUrl</c> is required.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>The created <see cref="Link"/>.</returns>
    /// <exception cref="ReroutException">On any API or transport failure.</exception>
    public Task<Link> CreateAsync(CreateLinkInput input, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<Link>(
            new ReroutRequest
            {
                Method = HttpMethod.Post,
                Path = "/v1/links",
                Body = input,
            },
            cancellationToken);
    }

    /// <summary>
    /// Create many links in a single request. Returns a partial-success
    /// envelope — inspect each <see cref="BatchLinkResult.Ok"/> for the per-item
    /// outcome.
    /// </summary>
    /// <param name="links">The links to create, in order.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <exception cref="ReroutException">On any API or transport failure.</exception>
    public Task<BatchCreateLinksResult> CreateBatchAsync(
        IReadOnlyList<BatchLinkInput> links,
        CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<BatchCreateLinksResult>(
            new ReroutRequest
            {
                Method = HttpMethod.Post,
                Path = "/v1/links/batch",
                Body = new Dictionary<string, object?> { ["links"] = links },
            },
            cancellationToken);
    }

    /// <summary>List links in the project, newest first.</summary>
    /// <param name="cursor">Pagination cursor from a previous call's <c>NextCursor</c>.</param>
    /// <param name="limit">Page size. Server-side default and maximum apply.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    public Task<ListLinksResult> ListAsync(
        long? cursor = null,
        int? limit = null,
        CancellationToken cancellationToken = default)
    {
        var query = new List<KeyValuePair<string, string>>();
        if (cursor is { } c)
        {
            query.Add(new KeyValuePair<string, string>("cursor", c.ToString(CultureInfo.InvariantCulture)));
        }

        if (limit is { } l)
        {
            query.Add(new KeyValuePair<string, string>("limit", l.ToString(CultureInfo.InvariantCulture)));
        }

        return _http.SendAsync<ListLinksResult>(
            new ReroutRequest
            {
                Method = HttpMethod.Get,
                Path = "/v1/links",
                Query = query,
            },
            cancellationToken);
    }

    /// <summary>Fetch a single link by its short code.</summary>
    public Task<Link> GetAsync(string code, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<Link>(
            new ReroutRequest
            {
                Method = HttpMethod.Get,
                Path = $"/v1/links/{EncodeCode(code)}",
            },
            cancellationToken);
    }

    /// <summary>
    /// Patch a link. Only fields set on <paramref name="input"/> are sent — see
    /// <see cref="UpdateLinkInput"/> for the omit / set / clear semantics.
    /// </summary>
    /// <exception cref="ReroutException">
    /// Thrown client-side with code <c>empty_update</c> when
    /// <paramref name="input"/> has no fields to send — the request never
    /// reaches the API.
    /// </exception>
    public Task<Link> UpdateAsync(string code, UpdateLinkInput input, CancellationToken cancellationToken = default)
    {
        if (input.IsEmpty)
        {
            throw new ReroutException(
                "empty_update",
                "UpdateLinkInput has no fields to send. Set at least one field.",
                0,
                path: $"/v1/links/{code}");
        }

        return _http.SendAsync<Link>(
            new ReroutRequest
            {
                Method = HttpMethod.Patch,
                Path = $"/v1/links/{EncodeCode(code)}",
                Body = input.ToPayload(),
            },
            cancellationToken);
    }

    /// <summary>
    /// Soft-delete a link. The short URL stops redirecting and drops out of
    /// list results.
    /// </summary>
    public Task<DeleteResult> DeleteAsync(string code, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<DeleteResult>(
            new ReroutRequest
            {
                Method = HttpMethod.Delete,
                Path = $"/v1/links/{EncodeCode(code)}",
            },
            cancellationToken);
    }

    /// <summary>Per-link click statistics across the requested window.</summary>
    /// <param name="code">The short code.</param>
    /// <param name="days">Window size in days. Defaults to 30.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    public Task<LinkStats> StatsAsync(string code, int days = 30, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<LinkStats>(
            new ReroutRequest
            {
                Method = HttpMethod.Get,
                Path = $"/v1/links/{EncodeCode(code)}/stats",
                Query = new[]
                {
                    new KeyValuePair<string, string>("days", days.ToString(CultureInfo.InvariantCulture)),
                },
            },
            cancellationToken);
    }

    internal static string EncodeCode(string code) => Uri.EscapeDataString(code);
}
