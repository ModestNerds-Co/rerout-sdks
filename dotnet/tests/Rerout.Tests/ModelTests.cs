using Rerout.Models;
using Xunit;

namespace Rerout.Tests;

public sealed class ModelTests
{
    [Fact]
    public void Optional_Default_IsUnset()
    {
        var value = default(Optional<string>);

        Assert.False(value.HasValue);
        Assert.Equal(Optional<string>.Unset, value);
    }

    [Fact]
    public void Optional_Set_CarriesValue()
    {
        var value = Optional<string>.Set("hello");

        Assert.True(value.HasValue);
        Assert.Equal("hello", value.Value);
    }

    [Fact]
    public void Optional_SetNull_HasValueButValueIsNull()
    {
        var value = Optional<string?>.Set(null);

        Assert.True(value.HasValue);
        Assert.Null(value.Value);
    }

    [Fact]
    public void Optional_TryGetValue_ReflectsHasValue()
    {
        Assert.True(Optional<int>.Set(7).TryGetValue(out var set));
        Assert.Equal(7, set);

        Assert.False(Optional<int>.Unset.TryGetValue(out _));
    }

    [Fact]
    public void Optional_ImplicitConversion_ProducesSet()
    {
        Optional<string> value = "implicit";

        Assert.True(value.HasValue);
        Assert.Equal("implicit", value.Value);
    }

    [Fact]
    public void UpdateLinkInput_Empty_IsEmpty()
    {
        Assert.True(new UpdateLinkInput().IsEmpty);
    }

    [Fact]
    public void UpdateLinkInput_WithOneField_IsNotEmpty()
    {
        var input = new UpdateLinkInput { IsActive = Optional<bool>.Set(false) };

        Assert.False(input.IsEmpty);
    }

    [Fact]
    public void UpdateLinkInput_ToPayload_OmitsUnsetFields()
    {
        var input = new UpdateLinkInput
        {
            TargetUrl = Optional<string>.Set("https://example.com"),
        };

        var payload = input.ToPayload();

        Assert.Single(payload);
        Assert.Equal("https://example.com", payload["target_url"]);
        Assert.False(payload.ContainsKey("seo_title"));
    }

    [Fact]
    public void UpdateLinkInput_ToPayload_IncludesExplicitNull()
    {
        var input = new UpdateLinkInput
        {
            SeoTitle = Optional<string?>.Set(null),
            ExpiresAt = Optional<long?>.Set(null),
        };

        var payload = input.ToPayload();

        Assert.True(payload.ContainsKey("seo_title"));
        Assert.Null(payload["seo_title"]);
        Assert.True(payload.ContainsKey("expires_at"));
        Assert.Null(payload["expires_at"]);
    }
}
