using System.Text.Json;
using System.Text.Json.Serialization;

namespace Rerout.Internal;

/// <summary>
/// Shared <see cref="JsonSerializerOptions"/> for the SDK. The Rerout API speaks
/// <c>snake_case</c>; response models carry explicit
/// <see cref="JsonPropertyNameAttribute"/> annotations, while loosely-typed
/// payloads (the <c>PATCH</c> body dictionary) rely on the naming policy.
/// </summary>
internal static class ReroutJson
{
    /// <summary>The single, reusable serializer configuration.</summary>
    public static readonly JsonSerializerOptions Options = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        DictionaryKeyPolicy = JsonNamingPolicy.SnakeCaseLower,
        PropertyNameCaseInsensitive = true,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
    };
}
