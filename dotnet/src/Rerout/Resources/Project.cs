using System.Collections.Generic;
using System.Globalization;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Rerout.Internal;
using Rerout.Models;

namespace Rerout.Resources;

/// <summary>
/// Project-level operations: aggregate analytics and current-project info.
/// Reached through <see cref="ReroutClient.Project"/>.
/// </summary>
public sealed class Project
{
    private readonly ReroutHttpHandler _http;

    internal Project(ReroutHttpHandler http) => _http = http;

    /// <summary>Aggregate stats across every link in the project.</summary>
    /// <param name="days">Window size in days. Defaults to 30.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    public Task<ProjectStats> StatsAsync(int days = 30, CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<ProjectStats>(
            new ReroutRequest
            {
                Method = HttpMethod.Get,
                Path = "/v1/projects/me/stats",
                Query = new[]
                {
                    new KeyValuePair<string, string>("days", days.ToString(CultureInfo.InvariantCulture)),
                },
            },
            cancellationToken);
    }

    /// <summary>Info about the project that owns the current API key.</summary>
    public Task<ProjectInfo> MeAsync(CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<ProjectInfo>(
            new ReroutRequest
            {
                Method = HttpMethod.Get,
                Path = "/v1/projects/me",
            },
            cancellationToken);
    }
}
