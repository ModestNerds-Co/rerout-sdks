using System;
using Rerout.Internal;
using Rerout.Resources;

namespace Rerout;

/// <summary>
/// Official .NET client for the Rerout branded-link API.
/// </summary>
/// <remarks>
/// <para>
/// Branded link infrastructure on Cloudflare — create short links, render QR
/// codes, read analytics, and verify webhook signatures.
/// </para>
/// <para>
/// The client owns an <see cref="System.Net.Http.HttpClient"/> unless one is
/// supplied via <see cref="ReroutClientOptions.HttpClient"/>. When the SDK owns
/// it, dispose the client (or call <see cref="Dispose"/>) to release the
/// connection pool. A single instance is thread-safe and should be reused for
/// the lifetime of the application.
/// </para>
/// </remarks>
/// <example>
/// <code>
/// using var rerout = new ReroutClient("rrk_…");
///
/// var link = await rerout.Links.CreateAsync(new CreateLinkInput
/// {
///     TargetUrl = "https://example.com/q4-sale",
/// });
/// Console.WriteLine(link.ShortUrl);
/// </code>
/// </example>
public sealed class ReroutClient : IDisposable
{
    private readonly ReroutHttpHandler _http;
    private bool _disposed;

    /// <summary>
    /// Create a client with the given project API key and default options.
    /// </summary>
    /// <param name="apiKey">Project API key (<c>rrk_…</c>). Required.</param>
    /// <exception cref="ReroutException">
    /// Thrown with code <c>missing_api_key</c> when <paramref name="apiKey"/> is
    /// null, empty, or whitespace.
    /// </exception>
    public ReroutClient(string apiKey)
        : this(apiKey, new ReroutClientOptions())
    {
    }

    /// <summary>
    /// Create a client with the given project API key and explicit options.
    /// </summary>
    /// <param name="apiKey">Project API key (<c>rrk_…</c>). Required.</param>
    /// <param name="options">Transport and base-URL configuration.</param>
    /// <exception cref="ReroutException">
    /// Thrown with code <c>missing_api_key</c> when <paramref name="apiKey"/> is
    /// null, empty, or whitespace.
    /// </exception>
    public ReroutClient(string apiKey, ReroutClientOptions options)
    {
        if (string.IsNullOrWhiteSpace(apiKey))
        {
            throw new ReroutException(
                "missing_api_key",
                "A project API key is required to construct ReroutClient.",
                0);
        }

        ArgumentNullException.ThrowIfNull(options);

        _http = new ReroutHttpHandler(apiKey, options);
        Links = new Links(_http);
        Project = new Project(_http);
        Qr = new Qr(_http);
        Webhooks = new Resources.Webhooks(_http);
    }

    /// <summary>Link operations: create, list, get, update, delete, stats.</summary>
    public Links Links { get; }

    /// <summary>Project-level operations: aggregate stats and current-project info.</summary>
    public Project Project { get; }

    /// <summary>QR helpers — pure URL builder and authenticated SVG fetch.</summary>
    public Qr Qr { get; }

    /// <summary>Webhook endpoint management: create, list, delete.</summary>
    public Resources.Webhooks Webhooks { get; }

    /// <summary>The resolved API base URL, with trailing slashes trimmed.</summary>
    public string BaseUrl => _http.BaseUrl;

    /// <summary>
    /// Release the underlying <see cref="System.Net.Http.HttpClient"/> when the
    /// SDK created it. A caller-supplied client is left untouched.
    /// </summary>
    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        _http.Dispose();
    }
}
