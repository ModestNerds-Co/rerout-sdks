using System;
using System.Collections.Generic;
using System.Globalization;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Rerout.Internal;

/// <summary>
/// Describes one HTTP request the SDK wants to make. Built by the resource
/// namespaces and handed to <see cref="ReroutHttpHandler"/>.
/// </summary>
internal sealed class ReroutRequest
{
    public required HttpMethod Method { get; init; }

    /// <summary>Path beginning with <c>/</c>, relative to the base URL.</summary>
    public required string Path { get; init; }

    /// <summary>Query-string pairs. Emitted only when non-empty.</summary>
    public IReadOnlyList<KeyValuePair<string, string>>? Query { get; init; }

    /// <summary>
    /// Body to serialize as JSON. <c>null</c> means no body — and no
    /// <c>Content-Type</c> header is sent.
    /// </summary>
    public object? Body { get; init; }

    /// <summary>
    /// When <c>true</c> the raw response text is returned to the caller instead
    /// of being parsed as JSON. Used by the QR SVG endpoint.
    /// </summary>
    public bool RawText { get; init; }
}

/// <summary>
/// The SDK's HTTP transport. Owns auth headers, query building, timeout
/// enforcement, error translation, and JSON (de)serialization.
/// </summary>
internal sealed class ReroutHttpHandler : IDisposable
{
    private readonly HttpClient _http;
    private readonly bool _ownsHttpClient;
    private readonly string _apiKey;
    private readonly string _baseUrl;
    private readonly TimeSpan _timeout;

    public ReroutHttpHandler(string apiKey, ReroutClientOptions options)
    {
        _apiKey = apiKey;
        _baseUrl = TrimTrailingSlashes(options.BaseUrl);
        _timeout = options.Timeout;

        if (options.HttpClient is not null)
        {
            _http = options.HttpClient;
            _ownsHttpClient = false;
        }
        else
        {
            _http = new HttpClient();
            _ownsHttpClient = true;
        }
    }

    /// <summary>The resolved base URL, with trailing slashes trimmed.</summary>
    public string BaseUrl => _baseUrl;

    /// <summary>The project API key, exposed for the QR resource's signed fetch.</summary>
    public string ApiKey => _apiKey;

    /// <summary>Trim every trailing <c>/</c> from a base URL.</summary>
    public static string TrimTrailingSlashes(string value)
    {
        var end = value.Length;
        while (end > 0 && value[end - 1] == '/')
        {
            end--;
        }

        return value[..end];
    }

    /// <summary>Execute a request and deserialize the JSON body into <typeparamref name="T"/>.</summary>
    public async Task<T> SendAsync<T>(ReroutRequest request, CancellationToken cancellationToken)
    {
        var text = await SendCoreAsync(request, cancellationToken).ConfigureAwait(false);

        if (request.RawText)
        {
            return (T)(object)text;
        }

        if (string.IsNullOrEmpty(text))
        {
            // 2xx with an empty body — only valid when T tolerates default.
            return default!;
        }

        try
        {
            var parsed = JsonSerializer.Deserialize<T>(text, ReroutJson.Options);
            if (parsed is null)
            {
                throw new ReroutException(
                    "unexpected_response",
                    "Rerout returned a JSON null where an object was expected.",
                    200,
                    path: request.Path);
            }

            return parsed;
        }
        catch (JsonException ex)
        {
            throw new ReroutException(
                "unexpected_response",
                "Rerout returned a non-JSON success body.",
                200,
                path: request.Path,
                details: text,
                innerException: ex);
        }
    }

    private async Task<string> SendCoreAsync(ReroutRequest request, CancellationToken cancellationToken)
    {
        using var message = BuildMessage(request);

        using var timeoutCts = new CancellationTokenSource(_timeout);
        using var linked = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken, timeoutCts.Token);

        HttpResponseMessage response;
        try
        {
            response = await _http.SendAsync(message, HttpCompletionOption.ResponseContentRead, linked.Token)
                .ConfigureAwait(false);
        }
        catch (OperationCanceledException) when (timeoutCts.IsCancellationRequested && !cancellationToken.IsCancellationRequested)
        {
            throw new ReroutException(
                "timeout",
                $"Request to Rerout timed out after {_timeout.TotalSeconds:0.#}s.",
                0,
                path: request.Path);
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            throw;
        }
        catch (TaskCanceledException ex)
        {
            // .NET surfaces HttpClient timeouts as TaskCanceledException.
            throw new ReroutException(
                "timeout",
                "Request to Rerout timed out.",
                0,
                path: request.Path,
                innerException: ex);
        }
        catch (HttpRequestException ex)
        {
            throw new ReroutException(
                "network_error",
                ex.Message.Length > 0 ? ex.Message : "Request to Rerout failed before the server replied.",
                0,
                path: request.Path,
                innerException: ex);
        }

        using (response)
        {
            var body = response.Content is null
                ? string.Empty
                : await response.Content.ReadAsStringAsync(linked.Token).ConfigureAwait(false);

            if (!response.IsSuccessStatusCode)
            {
                throw TranslateError((int)response.StatusCode, body, request.Path);
            }

            return body;
        }
    }

    private HttpRequestMessage BuildMessage(ReroutRequest request)
    {
        var uri = new StringBuilder(_baseUrl);
        uri.Append(request.Path);
        if (request.Query is { Count: > 0 } query)
        {
            var first = true;
            foreach (var pair in query)
            {
                uri.Append(first ? '?' : '&');
                first = false;
                uri.Append(Uri.EscapeDataString(pair.Key));
                uri.Append('=');
                uri.Append(Uri.EscapeDataString(pair.Value));
            }
        }

        var message = new HttpRequestMessage(request.Method, uri.ToString());
        message.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);
        message.Headers.Accept.ParseAdd(request.RawText ? "image/svg+xml, text/html" : "application/json");

        if (request.Body is not null)
        {
            var json = SerializeBody(request.Body);
            message.Content = new StringContent(json, Encoding.UTF8, "application/json");
        }

        return message;
    }

    private static string SerializeBody(object body) => body switch
    {
        IReadOnlyDictionary<string, object?> dict => JsonSerializer.Serialize(dict, ReroutJson.Options),
        _ => JsonSerializer.Serialize(body, body.GetType(), ReroutJson.Options),
    };

    private static ReroutException TranslateError(int status, string body, string path)
    {
        if (string.IsNullOrWhiteSpace(body))
        {
            return new ReroutException(
                SyntheticCodeForStatus(status),
                $"Rerout returned HTTP {status.ToString(CultureInfo.InvariantCulture)} with no body.",
                status,
                path: path);
        }

        try
        {
            using var doc = JsonDocument.Parse(body);
            var root = doc.RootElement;
            if (root.ValueKind != JsonValueKind.Object)
            {
                return new ReroutException(
                    SyntheticCodeForStatus(status),
                    $"Rerout returned HTTP {status.ToString(CultureInfo.InvariantCulture)} (non-object body).",
                    status,
                    path: path,
                    details: body);
            }

            var code = GetString(root, "code") ?? SyntheticCodeForStatus(status);
            var message = GetString(root, "message")
                ?? GetString(root, "error")
                ?? $"Rerout returned HTTP {status.ToString(CultureInfo.InvariantCulture)}.";
            var timestamp = GetString(root, "timestamp");
            var errorPath = GetString(root, "path") ?? path;
            object? details = root.TryGetProperty("details", out var d)
                ? d.Clone()
                : body;

            return new ReroutException(code, message, status, path: errorPath, timestamp: timestamp, details: details);
        }
        catch (JsonException)
        {
            return new ReroutException(
                SyntheticCodeForStatus(status),
                $"Rerout returned HTTP {status.ToString(CultureInfo.InvariantCulture)} (non-JSON body).",
                status,
                path: path,
                details: body);
        }
    }

    private static string? GetString(JsonElement obj, string name) =>
        obj.TryGetProperty(name, out var el) && el.ValueKind == JsonValueKind.String
            ? el.GetString()
            : null;

    /// <summary>Map an HTTP status with no usable body to a synthetic error code.</summary>
    public static string SyntheticCodeForStatus(int status) => status switch
    {
        401 => "unauthorized",
        403 => "forbidden",
        404 => "not_found",
        429 => "rate_limited",
        >= 500 and < 600 => "server_error",
        >= 400 and < 500 => "client_error",
        _ => "client_error",
    };

    public void Dispose()
    {
        if (_ownsHttpClient)
        {
            _http.Dispose();
        }
    }
}
