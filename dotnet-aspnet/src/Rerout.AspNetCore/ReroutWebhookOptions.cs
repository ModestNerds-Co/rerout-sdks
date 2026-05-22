using Rerout.Webhooks;

namespace Rerout.AspNetCore;

/// <summary>
/// Configuration for the Rerout webhook endpoint. Resolved from DI by the
/// webhook middleware.
/// </summary>
public sealed class ReroutWebhookOptions
{
    /// <summary>
    /// The endpoint signing secret (<c>whsec_…</c>) from the Rerout dashboard.
    /// Required — the middleware rejects every request when this is blank.
    /// </summary>
    public string SigningSecret { get; set; } = string.Empty;

    /// <summary>
    /// Timestamp tolerance window in seconds. Defaults to five minutes. Set to
    /// <c>0</c> to disable the replay-protection staleness check.
    /// </summary>
    public int ToleranceSeconds { get; set; } = SignatureVerifier.DefaultToleranceSeconds;
}
