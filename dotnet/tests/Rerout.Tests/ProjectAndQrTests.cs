using System;
using System.Net;
using System.Threading.Tasks;
using Rerout;
using Rerout.Models;
using Xunit;

namespace Rerout.Tests;

public sealed class ProjectAndQrTests
{
    [Fact]
    public async Task Project_Stats_Success_ReturnsAggregateStats()
    {
        var handler = StubHttpMessageHandler.Json("""
            {"days":7,"total_clicks":500,"qr_scans":120,
             "daily":[{"day":1716200000,"clicks":50,"qr_scans":10}],
             "countries":[{"value":"ZA","clicks":300}],
             "referrers":[],"devices":[],"browsers":[],"top_codes":[{"value":"q4","clicks":200}]}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var stats = await client.Project.StatsAsync(days: 7);

        Assert.Equal(7, stats.Days);
        Assert.Equal(500, stats.TotalClicks);
        Assert.Equal(120, stats.QrScans);
        Assert.Single(stats.Daily);
        Assert.Equal(50, stats.Daily[0].Clicks);
        Assert.Equal("q4", stats.TopCodes[0].Value);
    }

    [Fact]
    public async Task Project_Stats_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.TooManyRequests, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Project.StatsAsync());

        Assert.Equal("rate_limited", ex.Code);
        Assert.True(ex.IsRateLimited);
    }

    [Fact]
    public async Task Project_Me_Success_ReturnsProjectInfo()
    {
        var handler = StubHttpMessageHandler.Json("""
            {"id":"proj_123","name":"Acme","slug":"acme"}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var info = await client.Project.MeAsync();

        Assert.Equal("proj_123", info.Id);
        Assert.Equal("Acme", info.Name);
        Assert.Equal("acme", info.Slug);
        Assert.EndsWith("/v1/projects/me", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Project_Me_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.Unauthorized, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Project.MeAsync());

        Assert.Equal("unauthorized", ex.Code);
    }

    [Fact]
    public void Qr_Url_BareCode_HasNoQueryString()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var url = client.Qr.Url("q4");

        Assert.Equal("https://api.rerout.co/v1/links/q4/qr", url);
    }

    [Fact]
    public void Qr_Url_EmitsEveryOption()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var url = client.Qr.Url("q4", new QrOptions
        {
            Size = 12,
            Margin = 2,
            Ecc = "H",
            Domain = "go.brand.com",
            Refresh = "v2",
        });

        Assert.Contains("size=12", url, StringComparison.Ordinal);
        Assert.Contains("margin=2", url, StringComparison.Ordinal);
        Assert.Contains("ecc=H", url, StringComparison.Ordinal);
        Assert.Contains("domain=go.brand.com", url, StringComparison.Ordinal);
        Assert.Contains("refresh=v2", url, StringComparison.Ordinal);
    }

    [Fact]
    public void Qr_Url_HonoursCustomBaseUrl()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler, baseUrl: "https://staging.rerout.co/");

        var url = client.Qr.Url("q4");

        Assert.Equal("https://staging.rerout.co/v1/links/q4/qr", url);
    }

    [Fact]
    public void Qr_Url_RefreshAlwaysEmitsOne()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var url = client.Qr.Url("q4", new QrOptions { RefreshAlways = true });

        Assert.Contains("refresh=1", url, StringComparison.Ordinal);
    }

    [Fact]
    public void Qr_Url_RefreshTokenWinsOverRefreshAlways()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var url = client.Qr.Url("q4", new QrOptions { Refresh = "v3", RefreshAlways = true });

        Assert.Contains("refresh=v3", url, StringComparison.Ordinal);
        Assert.DoesNotContain("refresh=1", url, StringComparison.Ordinal);
    }

    [Fact]
    public void Qr_Url_EncodesCodeWithSpecialCharacters()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var url = client.Qr.Url("go/promo");

        Assert.Contains("/v1/links/go%2Fpromo/qr", url, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Qr_Svg_Success_ReturnsSvgText()
    {
        const string svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>";
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.OK, svg, "image/svg+xml");
        using var client = TestHelpers.ClientWith(handler);

        var body = await client.Qr.SvgAsync("q4", new QrOptions { Size = 8 });

        Assert.Equal(svg, body);
        Assert.Contains("size=8", handler.Requests[0].Uri.Query, StringComparison.Ordinal);
        Assert.Contains("image/svg+xml", handler.Requests[0].Headers["Accept"], StringComparison.Ordinal);
    }

    [Fact]
    public async Task Qr_Svg_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.NotFound, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Qr.SvgAsync("missing"));

        Assert.Equal("not_found", ex.Code);
    }
}
