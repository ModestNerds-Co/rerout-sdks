using System;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Rerout.Internal;
using Rerout.Models;

namespace Rerout.Resources;

/// <summary>
/// QR helpers — a pure URL builder and an authenticated SVG fetch. Reached
/// through <see cref="ReroutClient.Qr"/>.
/// </summary>
public sealed class Qr
{
    private readonly ReroutHttpHandler _http;

    internal Qr(ReroutHttpHandler http) => _http = http;

    /// <summary>
    /// Build the URL the Rerout API serves the QR SVG from. Pure — this makes
    /// no network call.
    /// </summary>
    /// <remarks>
    /// The endpoint is API-key authenticated, so a bare <c>&lt;img&gt;</c> tag
    /// cannot load it directly — proxy the request server-side, or use
    /// <see cref="SvgAsync"/> to fetch the rendered body with the bearer token
    /// attached.
    /// </remarks>
    /// <param name="code">The short code to encode.</param>
    /// <param name="options">Optional rendering parameters.</param>
    /// <returns>An absolute URL string.</returns>
    public string Url(string code, QrOptions? options = null)
    {
        var builder = new StringBuilder(_http.BaseUrl);
        builder.Append("/v1/links/");
        builder.Append(Uri.EscapeDataString(code));
        builder.Append("/qr");

        if (options is not null)
        {
            var first = true;
            foreach (var pair in options.ToQueryParameters())
            {
                builder.Append(first ? '?' : '&');
                first = false;
                builder.Append(Uri.EscapeDataString(pair.Key));
                builder.Append('=');
                builder.Append(Uri.EscapeDataString(pair.Value));
            }
        }

        return builder.ToString();
    }

    /// <summary>
    /// Fetch the QR as an SVG string. Hits the same endpoint as
    /// <see cref="Url"/> but attaches the bearer token and returns the rendered
    /// body.
    /// </summary>
    public Task<string> SvgAsync(string code, QrOptions? options = null, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<string>(
            new ReroutRequest
            {
                Method = HttpMethod.Get,
                Path = $"/v1/links/{Uri.EscapeDataString(code)}/qr",
                Query = options?.ToQueryParameters(),
                RawText = true,
            },
            cancellationToken);
    }
}
