using System;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using Rerout;
using Xunit;

namespace Rerout.Tests;

public sealed class ConstructorTests
{
    [Fact]
    public void Constructor_WithValidApiKey_Succeeds()
    {
        using var client = new ReroutClient(TestHelpers.ApiKey);

        Assert.NotNull(client.Links);
        Assert.NotNull(client.Project);
        Assert.NotNull(client.Qr);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void Constructor_WithBlankApiKey_ThrowsMissingApiKey(string apiKey)
    {
        var ex = Assert.Throws<ReroutException>(() => new ReroutClient(apiKey));

        Assert.Equal("missing_api_key", ex.Code);
        Assert.Equal(0, ex.Status);
    }

    [Fact]
    public void Constructor_WithNullApiKey_ThrowsMissingApiKey()
    {
        var ex = Assert.Throws<ReroutException>(() => new ReroutClient(null!));

        Assert.Equal("missing_api_key", ex.Code);
    }

    [Fact]
    public void Constructor_WithNullOptions_Throws()
    {
        Assert.Throws<ArgumentNullException>(() => new ReroutClient(TestHelpers.ApiKey, null!));
    }

    [Fact]
    public void BaseUrl_DefaultsToProductionApi()
    {
        using var client = new ReroutClient(TestHelpers.ApiKey);

        Assert.Equal("https://api.rerout.co", client.BaseUrl);
    }

    [Theory]
    [InlineData("https://staging.rerout.co/", "https://staging.rerout.co")]
    [InlineData("https://staging.rerout.co///", "https://staging.rerout.co")]
    [InlineData("https://staging.rerout.co", "https://staging.rerout.co")]
    public void BaseUrl_TrimsTrailingSlashes(string input, string expected)
    {
        using var client = new ReroutClient(TestHelpers.ApiKey, new ReroutClientOptions
        {
            BaseUrl = input,
        });

        Assert.Equal(expected, client.BaseUrl);
    }

    [Fact]
    public async Task Dispose_DoesNotDisposeCallerSuppliedHttpClient()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        var http = new HttpClient(handler);

        var client = new ReroutClient(TestHelpers.ApiKey, new ReroutClientOptions { HttpClient = http });
        client.Dispose();

        // The caller's HttpClient must still work after the SDK is disposed.
        using var probe = new ReroutClient(TestHelpers.ApiKey, new ReroutClientOptions { HttpClient = http });
        var link = await probe.Links.GetAsync("q4");
        Assert.Equal("q4", link.Code);
    }

    [Fact]
    public void Dispose_IsIdempotent()
    {
        var client = new ReroutClient(TestHelpers.ApiKey);
        client.Dispose();
        client.Dispose();
    }
}
