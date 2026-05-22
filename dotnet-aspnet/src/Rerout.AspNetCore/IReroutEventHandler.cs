using System.Threading;
using System.Threading.Tasks;

namespace Rerout.AspNetCore;

/// <summary>
/// Handles verified inbound Rerout webhook events.
/// </summary>
/// <remarks>
/// <para>
/// Register an implementation with <see cref="DependencyInjection.ReroutServiceCollectionExtensions"/>:
/// <c>services.AddRerout(…).AddReroutWebhookHandler&lt;MyHandler&gt;()</c>, or
/// register it directly against the DI container. The webhook middleware
/// resolves it per request from a scoped service provider, so the handler may
/// depend on scoped services (e.g. a <c>DbContext</c>).
/// </para>
/// <para>
/// The middleware only calls <see cref="HandleAsync"/> after the request
/// signature has been verified and the body parsed — implementations can trust
/// the event is authentic.
/// </para>
/// </remarks>
public interface IReroutEventHandler
{
    /// <summary>Process a single verified webhook event.</summary>
    /// <param name="webhookEvent">The verified, deserialized event.</param>
    /// <param name="cancellationToken">Cancelled if the request is aborted.</param>
    Task HandleAsync(ReroutWebhookEvent webhookEvent, CancellationToken cancellationToken);
}
