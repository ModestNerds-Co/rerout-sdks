using System;

namespace Rerout;

/// <summary>
/// Exception thrown for any failed Rerout API call — bad request, auth issue,
/// rate-limit, or network-level failure.
/// </summary>
/// <remarks>
/// <para>
/// The <see cref="Code"/> field carries the stable string identifier returned by
/// the Rerout API (e.g. <c>bad_target_url</c>, <c>rate_limited</c>,
/// <c>not_found</c>) so callers can branch on it without parsing
/// <see cref="Exception.Message"/>.
/// </para>
/// <para>
/// For network or non-JSON failures the <see cref="Code"/> is one of the synthetic
/// client-side values: <c>network_error</c>, <c>timeout</c>,
/// <c>unexpected_response</c>, <c>unauthorized</c>, <c>forbidden</c>,
/// <c>not_found</c>, <c>rate_limited</c>, <c>server_error</c>, <c>client_error</c>,
/// <c>missing_api_key</c>.
/// </para>
/// </remarks>
public sealed class ReroutException : Exception
{
    /// <summary>Creates a <see cref="ReroutException"/>.</summary>
    public ReroutException(
        string code,
        string message,
        int status,
        string? path = null,
        string? timestamp = null,
        object? details = null,
        Exception? innerException = null)
        : base(message, innerException)
    {
        Code = code;
        Status = status;
        Path = path;
        Timestamp = timestamp;
        Details = details;
    }

    /// <summary>Stable error code, from the API or synthesized client-side.</summary>
    public string Code { get; }

    /// <summary>HTTP status code, or <c>0</c> when the request never reached the server.</summary>
    public int Status { get; }

    /// <summary>The API path that caused the error, when known.</summary>
    public string? Path { get; }

    /// <summary>The timestamp of the error (ISO 8601), when supplied by the API.</summary>
    public string? Timestamp { get; }

    /// <summary>
    /// The raw response body (parsed JSON object or string), or other debugging
    /// context relevant to the failure.
    /// </summary>
    public object? Details { get; }

    /// <summary><c>true</c> when the failure is HTTP 5xx — a server-side issue.</summary>
    public bool IsServerError => Status >= 500 && Status < 600;

    /// <summary><c>true</c> when the failure is HTTP 429 — caller should back off and retry.</summary>
    public bool IsRateLimited => Status == 429;

    /// <inheritdoc />
    public override string ToString()
    {
        return $"ReroutException(code: {Code}, status: {Status}, message: {Message}, path: {Path ?? "<null>"}, timestamp: {Timestamp ?? "<null>"})";
    }
}
