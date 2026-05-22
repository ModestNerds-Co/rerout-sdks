using System;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Routing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace Rerout.AspNetCore;

/// <summary>
/// Endpoint-routing helpers for mapping the Rerout webhook receiver.
/// </summary>
public static class ReroutWebhookEndpointRouteBuilderExtensions
{
    /// <summary>
    /// Map a <c>POST</c> endpoint that receives Rerout webhooks at
    /// <paramref name="pattern"/>.
    /// </summary>
    /// <remarks>
    /// The endpoint verifies the <c>X-Rerout-Signature</c> header, deserializes
    /// the event, and dispatches it to the registered
    /// <see cref="IReroutEventHandler"/>. It returns <c>200</c> on success,
    /// <c>401</c> for a bad signature, and <c>400</c> for a malformed body.
    /// </remarks>
    /// <param name="endpoints">The endpoint route builder.</param>
    /// <param name="pattern">The route pattern, e.g. <c>/webhooks/rerout</c>.</param>
    /// <returns>The mapped endpoint convention builder, for further customization.</returns>
    public static IEndpointConventionBuilder MapReroutWebhook(
        this IEndpointRouteBuilder endpoints,
        string pattern)
    {
        ArgumentNullException.ThrowIfNull(endpoints);
        ArgumentException.ThrowIfNullOrEmpty(pattern);

        return endpoints.MapPost(pattern, static async (HttpContext context) =>
        {
            var options = context.RequestServices.GetRequiredService<IOptions<ReroutWebhookOptions>>();
            var logger = context.RequestServices
                .GetRequiredService<ILoggerFactory>()
                .CreateLogger<ReroutWebhookMiddleware>();

            var middleware = new ReroutWebhookMiddleware(options, logger);
            await middleware.InvokeAsync(context).ConfigureAwait(false);
        })
        .WithName("ReroutWebhook")
        .WithDisplayName("Rerout webhook receiver");
    }
}
