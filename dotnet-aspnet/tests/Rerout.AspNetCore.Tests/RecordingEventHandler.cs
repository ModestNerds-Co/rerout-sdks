using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Rerout.AspNetCore.Tests;

/// <summary>
/// An <see cref="IReroutEventHandler"/> that records every event it receives,
/// for assertion in tests. Optionally throws to exercise the failure path.
/// </summary>
internal sealed class RecordingEventHandler : IReroutEventHandler
{
    /// <summary>Every event handled, in order.</summary>
    public List<ReroutWebhookEvent> Received { get; } = new();

    /// <summary>When set, the handler throws this on its next invocation.</summary>
    public Exception? ThrowOnHandle { get; set; }

    public Task HandleAsync(ReroutWebhookEvent webhookEvent, CancellationToken cancellationToken)
    {
        if (ThrowOnHandle is { } ex)
        {
            throw ex;
        }

        Received.Add(webhookEvent);
        return Task.CompletedTask;
    }
}
