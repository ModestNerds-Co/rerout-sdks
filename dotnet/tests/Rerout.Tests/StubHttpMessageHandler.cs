using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;

namespace Rerout.Tests;

/// <summary>
/// A test double for <see cref="HttpMessageHandler"/>. Captures every outbound
/// request and replays a queued or scripted response — no network ever touched.
/// </summary>
internal sealed class StubHttpMessageHandler : HttpMessageHandler
{
    private readonly Func<HttpRequestMessage, CancellationToken, Task<HttpResponseMessage>> _responder;

    /// <summary>Every request the handler observed, in order.</summary>
    public List<CapturedRequest> Requests { get; } = new();

    private StubHttpMessageHandler(Func<HttpRequestMessage, CancellationToken, Task<HttpResponseMessage>> responder)
    {
        _responder = responder;
    }

    /// <summary>Always reply with the same status, body, and content type.</summary>
    public static StubHttpMessageHandler Always(
        HttpStatusCode status,
        string body,
        string contentType = "application/json")
    {
        return new StubHttpMessageHandler((_, _) => Task.FromResult(BuildResponse(status, body, contentType)));
    }

    /// <summary>Reply with a JSON 200 body.</summary>
    public static StubHttpMessageHandler Json(string body) =>
        Always(HttpStatusCode.OK, body);

    /// <summary>Throw an <see cref="HttpRequestException"/> to simulate a network failure.</summary>
    public static StubHttpMessageHandler NetworkFailure(string message = "connection refused")
    {
        return new StubHttpMessageHandler((_, _) =>
            throw new HttpRequestException(message));
    }

    /// <summary>Hang until cancelled — used to exercise the client timeout path.</summary>
    public static StubHttpMessageHandler Hang()
    {
        return new StubHttpMessageHandler(async (_, token) =>
        {
            await Task.Delay(Timeout.Infinite, token).ConfigureAwait(false);
            throw new InvalidOperationException("unreachable");
        });
    }

    /// <summary>Drive the response with a custom callback for full control.</summary>
    public static StubHttpMessageHandler Custom(
        Func<HttpRequestMessage, HttpResponseMessage> responder)
    {
        return new StubHttpMessageHandler((req, _) => Task.FromResult(responder(req)));
    }

    protected override async Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        var captured = await CapturedRequest.FromAsync(request).ConfigureAwait(false);
        Requests.Add(captured);
        return await _responder(request, cancellationToken).ConfigureAwait(false);
    }

    private static HttpResponseMessage BuildResponse(HttpStatusCode status, string body, string contentType)
    {
        var response = new HttpResponseMessage(status)
        {
            Content = new StringContent(body, System.Text.Encoding.UTF8),
        };
        if (body.Length > 0)
        {
            response.Content.Headers.ContentType =
                new System.Net.Http.Headers.MediaTypeHeaderValue(contentType);
        }

        return response;
    }
}

/// <summary>An immutable snapshot of a request the stub handler saw.</summary>
internal sealed record CapturedRequest
{
    public required HttpMethod Method { get; init; }

    public required Uri Uri { get; init; }

    public required IReadOnlyDictionary<string, string> Headers { get; init; }

    public string? Body { get; init; }

    public string? ContentType { get; init; }

    public static async Task<CapturedRequest> FromAsync(HttpRequestMessage request)
    {
        var headers = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var header in request.Headers)
        {
            headers[header.Key] = string.Join(",", header.Value);
        }

        string? body = null;
        string? contentType = null;
        if (request.Content is not null)
        {
            body = await request.Content.ReadAsStringAsync().ConfigureAwait(false);
            contentType = request.Content.Headers.ContentType?.MediaType;
        }

        return new CapturedRequest
        {
            Method = request.Method,
            Uri = request.RequestUri!,
            Headers = headers,
            Body = body,
            ContentType = contentType,
        };
    }
}
