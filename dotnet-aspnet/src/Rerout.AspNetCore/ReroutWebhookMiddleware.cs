using System;
using System.IO;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Rerout.Webhooks;

namespace Rerout.AspNetCore;

/// <summary>
/// Terminal request handler for the Rerout webhook endpoint. Reads the raw
/// body, verifies the <c>X-Rerout-Signature</c> header, deserializes the event,
/// and dispatches it to the registered <see cref="IReroutEventHandler"/>.
/// </summary>
/// <remarks>
/// Response contract:
/// <list type="bullet">
/// <item><description><c>200 OK</c> — verified, parsed, and handled.</description></item>
/// <item><description><c>401 Unauthorized</c> — missing or invalid signature.</description></item>
/// <item><description><c>400 Bad Request</c> — body absent or not a valid event.</description></item>
/// <item><description><c>500</c> — left to the framework when the handler throws.</description></item>
/// </list>
/// </remarks>
public sealed class ReroutWebhookMiddleware
{
    /// <summary>The header Rerout signs every delivery with.</summary>
    public const string SignatureHeaderName = "X-Rerout-Signature";

    private readonly ReroutWebhookOptions _options;
    private readonly ILogger<ReroutWebhookMiddleware> _logger;

    /// <summary>Construct the middleware with resolved options and a logger.</summary>
    public ReroutWebhookMiddleware(
        IOptions<ReroutWebhookOptions> options,
        ILogger<ReroutWebhookMiddleware> logger)
    {
        ArgumentNullException.ThrowIfNull(options);
        _options = options.Value;
        _logger = logger;
    }

    /// <summary>Handle a single inbound webhook request.</summary>
    public async Task InvokeAsync(HttpContext context)
    {
        ArgumentNullException.ThrowIfNull(context);

        if (string.IsNullOrEmpty(_options.SigningSecret))
        {
            _logger.LogError(
                "Rerout webhook rejected: no signing secret is configured. "
                + "Call AddReroutWebhooks with the whsec_… secret.");
            await WriteStatusAsync(context, StatusCodes.Status401Unauthorized, "missing_secret").ConfigureAwait(false);
            return;
        }

        var rawBody = await ReadRawBodyAsync(context.Request).ConfigureAwait(false);

        var signature = context.Request.Headers[SignatureHeaderName].ToString();
        var verified = SignatureVerifier.Verify(
            rawBody,
            signature,
            _options.SigningSecret,
            _options.ToleranceSeconds);

        if (!verified)
        {
            _logger.LogWarning("Rerout webhook rejected: signature verification failed.");
            await WriteStatusAsync(context, StatusCodes.Status401Unauthorized, "invalid_signature").ConfigureAwait(false);
            return;
        }

        if (!TryParseEvent(rawBody, out var webhookEvent, out var parseError))
        {
            _logger.LogWarning("Rerout webhook rejected: {Error}.", parseError);
            await WriteStatusAsync(context, StatusCodes.Status400BadRequest, parseError).ConfigureAwait(false);
            return;
        }

        var handler = context.RequestServices.GetService<IReroutEventHandler>();
        if (handler is null)
        {
            _logger.LogError(
                "Rerout webhook verified but no IReroutEventHandler is registered. "
                + "Call AddReroutWebhookHandler<T>().");
            await WriteStatusAsync(context, StatusCodes.Status500InternalServerError, "no_handler").ConfigureAwait(false);
            return;
        }

        await handler.HandleAsync(webhookEvent, context.RequestAborted).ConfigureAwait(false);
        await WriteStatusAsync(context, StatusCodes.Status200OK, "ok").ConfigureAwait(false);
    }

    private static async Task<string> ReadRawBodyAsync(HttpRequest request)
    {
        request.EnableBuffering();
        request.Body.Position = 0;
        using var reader = new StreamReader(
            request.Body,
            Encoding.UTF8,
            detectEncodingFromByteOrderMarks: false,
            leaveOpen: true);
        var body = await reader.ReadToEndAsync().ConfigureAwait(false);
        request.Body.Position = 0;
        return body;
    }

    private static bool TryParseEvent(string rawBody, out ReroutWebhookEvent webhookEvent, out string error)
    {
        webhookEvent = default!;
        error = string.Empty;

        if (string.IsNullOrWhiteSpace(rawBody))
        {
            error = "empty_body";
            return false;
        }

        JsonDocument doc;
        try
        {
            doc = JsonDocument.Parse(rawBody);
        }
        catch (JsonException)
        {
            error = "invalid_json";
            return false;
        }

        using (doc)
        {
            var root = doc.RootElement;
            if (root.ValueKind != JsonValueKind.Object)
            {
                error = "not_an_object";
                return false;
            }

            var type = GetString(root, "type");
            if (string.IsNullOrEmpty(type))
            {
                error = "missing_type";
                return false;
            }

            if (!root.TryGetProperty("data", out var data) || data.ValueKind != JsonValueKind.Object)
            {
                error = "missing_data";
                return false;
            }

            var id = GetString(root, "id") ?? string.Empty;
            var createdAt = root.TryGetProperty("created_at", out var createdEl)
                && createdEl.ValueKind == JsonValueKind.Number
                && createdEl.TryGetInt64(out var parsed)
                ? parsed
                : 0L;

            webhookEvent = new ReroutWebhookEvent(type, id, createdAt, data);
            return true;
        }
    }

    private static string? GetString(JsonElement obj, string name) =>
        obj.TryGetProperty(name, out var el) && el.ValueKind == JsonValueKind.String
            ? el.GetString()
            : null;

    private static async Task WriteStatusAsync(HttpContext context, int statusCode, string code)
    {
        context.Response.StatusCode = statusCode;
        context.Response.ContentType = "application/json";
        var body = JsonSerializer.Serialize(new { code }, ReroutWebhookJson.Options);
        await context.Response.WriteAsync(body, context.RequestAborted).ConfigureAwait(false);
    }
}
