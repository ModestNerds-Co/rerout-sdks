using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Rerout.Internal;
using Rerout.Models;

namespace Rerout.Resources;

/// <summary>
/// Conversion tracking: record conversions against prior clicks. Reached through
/// <see cref="ReroutClient.Conversions"/>.
/// </summary>
public sealed class Conversions
{
    private readonly ReroutHttpHandler _http;

    internal Conversions(ReroutHttpHandler http) => _http = http;

    /// <summary>
    /// Record a conversion attributed to a prior click via its <c>ClickId</c>
    /// (<c>rrid</c>). Idempotent per <c>(ClickId, EventName)</c>: a repeat call
    /// returns <see cref="RecordedConversion.Duplicate"/> set to <c>true</c>
    /// without double-counting.
    /// </summary>
    /// <param name="input">The conversion to record. <c>ClickId</c> and <c>EventName</c> are required.</param>
    /// <param name="cancellationToken">Optional cancellation token.</param>
    /// <returns>Whether the conversion was recorded and whether it was a duplicate.</returns>
    /// <exception cref="ReroutException">On any API or transport failure.</exception>
    public Task<RecordedConversion> RecordAsync(
        RecordConversionInput input,
        CancellationToken cancellationToken = default)
    {
        return _http.SendAsync<RecordedConversion>(
            new ReroutRequest
            {
                Method = HttpMethod.Post,
                Path = "/v1/conversions",
                Body = input,
            },
            cancellationToken);
    }
}
