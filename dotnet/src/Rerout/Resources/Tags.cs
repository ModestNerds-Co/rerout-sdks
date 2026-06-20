using System;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Rerout.Internal;
using Rerout.Models;

namespace Rerout.Resources;

/// <summary>
/// Tag management: list, create, update, delete. Reached through
/// <see cref="ReroutClient.Tags"/>. The project is resolved from the API key —
/// there is no project id in the path.
/// </summary>
public sealed class Tags
{
    private readonly ReroutHttpHandler _http;

    internal Tags(ReroutHttpHandler http) => _http = http;

    /// <summary>
    /// List the project's tags with their live (non-deleted) link counts.
    /// </summary>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>The tags registered to the project, each as a <see cref="TagSummary"/>.</returns>
    /// <exception cref="ReroutException">On any API or transport failure.</exception>
    public Task<ListTagsResult> ListAsync(CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<ListTagsResult>(
            new ReroutRequest
            {
                Method = HttpMethod.Get,
                Path = "/v1/projects/me/tags",
            },
            cancellationToken);
    }

    /// <summary>
    /// Create a tag. <see cref="CreateTagInput.Color"/> is optional; the server
    /// validates it and defaults to <c>teal</c> when omitted.
    /// </summary>
    /// <param name="input">The tag to create. <c>Name</c> is required.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>The created tag (without a link count).</returns>
    /// <exception cref="ReroutException">On any API or transport failure.</exception>
    public Task<Tag> CreateAsync(CreateTagInput input, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<Tag>(
            new ReroutRequest
            {
                Method = HttpMethod.Post,
                Path = "/v1/projects/me/tags",
                Body = input,
            },
            cancellationToken);
    }

    /// <summary>
    /// Update a tag's name and/or color. Only the fields set on
    /// <paramref name="input"/> are sent; omitted fields are left unchanged.
    /// </summary>
    /// <param name="tagId">The tag identifier (<c>tag_…</c>).</param>
    /// <param name="input">The fields to change. At least one must be set.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>The updated tag (without a link count).</returns>
    /// <exception cref="ReroutException">
    /// Thrown client-side with code <c>empty_update</c> when
    /// <paramref name="input"/> has no fields to send — the request never reaches
    /// the API. (Mirrors <see cref="Links.UpdateAsync"/>.)
    /// </exception>
    public Task<Tag> UpdateAsync(string tagId, UpdateTagInput input, CancellationToken cancellationToken = default)
    {
        if (input.IsEmpty)
        {
            throw new ReroutException(
                "empty_update",
                "UpdateTagInput has no fields to send. Set at least one field.",
                0,
                path: $"/v1/projects/me/tags/{tagId}");
        }

        return _http.SendAsync<Tag>(
            new ReroutRequest
            {
                Method = HttpMethod.Patch,
                Path = $"/v1/projects/me/tags/{EncodeId(tagId)}",
                Body = input,
            },
            cancellationToken);
    }

    /// <summary>
    /// Delete a tag and drop its assignments from all links. Idempotent.
    /// </summary>
    /// <param name="tagId">The tag identifier (<c>tag_…</c>).</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>A <see cref="DeleteResult"/> with <c>Deleted = true</c>.</returns>
    /// <exception cref="ReroutException">On any API or transport failure.</exception>
    public Task<DeleteResult> DeleteAsync(string tagId, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<DeleteResult>(
            new ReroutRequest
            {
                Method = HttpMethod.Delete,
                Path = $"/v1/projects/me/tags/{EncodeId(tagId)}",
            },
            cancellationToken);
    }

    internal static string EncodeId(string tagId) => Uri.EscapeDataString(tagId);
}
