using System;
using System.Net.Http;

namespace Rerout;

/// <summary>
/// Configuration for <see cref="ReroutClient"/>. All fields except <see cref="ApiKey"/>
/// are optional and have sensible production defaults.
/// </summary>
public sealed record ReroutClientOptions
{
    /// <summary>Default production API URL.</summary>
    public const string DefaultBaseUrl = "https://api.rerout.co";

    /// <summary>Default per-request timeout (30 seconds).</summary>
    public static readonly TimeSpan DefaultTimeout = TimeSpan.FromSeconds(30);

    /// <summary>
    /// Override the API base URL. Defaults to <see cref="DefaultBaseUrl"/>. Trailing
    /// slashes are trimmed.
    /// </summary>
    public string BaseUrl { get; init; } = DefaultBaseUrl;

    /// <summary>Per-request timeout. Defaults to 30 seconds.</summary>
    public TimeSpan Timeout { get; init; } = DefaultTimeout;

    /// <summary>
    /// Inject a pre-configured <see cref="HttpClient"/>. Useful for tests, DI, and
    /// for callers who want to install their own delegating handlers. When supplied,
    /// the client's <see cref="HttpClient.Timeout"/> is left as the caller set it
    /// and <see cref="Timeout"/> is enforced via a per-request linked
    /// <see cref="System.Threading.CancellationToken"/>.
    /// </summary>
    public HttpClient? HttpClient { get; init; }
}
