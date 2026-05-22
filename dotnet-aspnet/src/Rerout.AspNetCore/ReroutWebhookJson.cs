using System.Text.Json;

namespace Rerout.AspNetCore;

/// <summary>
/// Shared <see cref="JsonSerializerOptions"/> for webhook payloads. The Rerout
/// API emits <c>snake_case</c>; event records carry explicit
/// <see cref="System.Text.Json.Serialization.JsonPropertyNameAttribute"/>
/// annotations but the policy is set for any loosely-typed access.
/// </summary>
internal static class ReroutWebhookJson
{
    /// <summary>The single, reusable serializer configuration.</summary>
    public static readonly JsonSerializerOptions Options = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        PropertyNameCaseInsensitive = true,
    };
}
