using System;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using Rerout;
using Rerout.Models;
using Xunit;

namespace Rerout.Tests;

public sealed class LinksTests
{
    [Fact]
    public async Task Create_Success_ReturnsLink()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var link = await client.Links.CreateAsync(new CreateLinkInput
        {
            TargetUrl = "https://example.com/q4-sale",
        });

        Assert.Equal("q4", link.Code);
        Assert.Equal("https://go.brand.com/q4", link.ShortUrl);
        Assert.True(link.IsActive);
        Assert.Equal(HttpMethod.Post, handler.Requests[0].Method);
        Assert.EndsWith("/v1/links", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Create_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(
            HttpStatusCode.BadRequest,
            """{"code":"bad_target_url","message":"https only."}""");
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Links.CreateAsync(new CreateLinkInput { TargetUrl = "http://x" }));

        Assert.Equal("bad_target_url", ex.Code);
    }

    [Fact]
    public async Task List_Success_ReturnsLinksAndCursor()
    {
        var handler = StubHttpMessageHandler.Json($$"""
            {"links":[{{TestHelpers.SampleLinkJson}}],"next_cursor":99}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Links.ListAsync(limit: 1);

        Assert.Single(result.Links);
        Assert.Equal(99, result.NextCursor);
        Assert.Equal("q4", result.Links[0].Code);
    }

    [Fact]
    public async Task List_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.Unauthorized, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.ListAsync());

        Assert.Equal("unauthorized", ex.Code);
    }

    [Fact]
    public async Task Get_Success_ReturnsLink()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var link = await client.Links.GetAsync("q4");

        Assert.Equal("q4", link.Code);
        Assert.Equal(HttpMethod.Get, handler.Requests[0].Method);
        Assert.EndsWith("/v1/links/q4", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Get_Error_ThrowsNotFound()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.NotFound, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.GetAsync("missing"));

        Assert.Equal("not_found", ex.Code);
        Assert.Equal(404, ex.Status);
    }

    [Fact]
    public async Task Update_Success_SendsPatchWithChangedFields()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.UpdateAsync("q4", new UpdateLinkInput
        {
            IsActive = Optional<bool>.Set(false),
            TargetUrl = Optional<string>.Set("https://example.com/new"),
        });

        Assert.Equal(HttpMethod.Patch, handler.Requests[0].Method);
        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.False(root.GetProperty("is_active").GetBoolean());
        Assert.Equal("https://example.com/new", root.GetProperty("target_url").GetString());
        Assert.False(root.TryGetProperty("seo_title", out _));
    }

    [Fact]
    public async Task Update_SetNull_SendsExplicitNull()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.UpdateAsync("q4", new UpdateLinkInput
        {
            SeoTitle = Optional<string?>.Set(null),
        });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        Assert.Equal(JsonValueKind.Null, doc.RootElement.GetProperty("seo_title").ValueKind);
    }

    [Fact]
    public async Task Update_EmptyInput_ThrowsClientSideWithoutHittingApi()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Links.UpdateAsync("q4", new UpdateLinkInput()));

        Assert.Equal("empty_update", ex.Code);
        Assert.Empty(handler.Requests);
    }

    [Fact]
    public async Task Update_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.NotFound, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Links.UpdateAsync("q4", new UpdateLinkInput { IsActive = Optional<bool>.Set(true) }));

        Assert.Equal("not_found", ex.Code);
    }

    [Fact]
    public async Task Delete_Success_ReturnsDeleteResult()
    {
        var handler = StubHttpMessageHandler.Json("""{"deleted":true}""");
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Links.DeleteAsync("q4");

        Assert.True(result.Deleted);
        Assert.Equal(HttpMethod.Delete, handler.Requests[0].Method);
    }

    [Fact]
    public async Task Delete_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.Forbidden, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.DeleteAsync("q4"));

        Assert.Equal("forbidden", ex.Code);
    }

    [Fact]
    public async Task Stats_Success_ReturnsLinkStats()
    {
        var handler = StubHttpMessageHandler.Json("""
            {"code":"q4","days":30,"total_clicks":120,"qr_scans":40,
             "countries":[{"value":"ZA","clicks":80}],"referrers":[]}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var stats = await client.Links.StatsAsync("q4");

        Assert.Equal("q4", stats.Code);
        Assert.Equal(120, stats.TotalClicks);
        Assert.Equal(40, stats.QrScans);
        Assert.Single(stats.Countries);
        Assert.Equal("ZA", stats.Countries[0].Value);
    }

    [Fact]
    public async Task Stats_DefaultsToThirtyDays()
    {
        var handler = StubHttpMessageHandler.Json("""
            {"code":"q4","days":30,"total_clicks":0,"qr_scans":0,"countries":[],"referrers":[]}
            """);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.StatsAsync("q4");

        Assert.Contains("days=30", handler.Requests[0].Uri.Query, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Stats_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.InternalServerError, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.StatsAsync("q4"));

        Assert.Equal("server_error", ex.Code);
    }

    [Theory]
    [InlineData("hello world", "hello%20world")]
    [InlineData("a+b", "a%2Bb")]
    [InlineData("café", "caf%C3%A9")]
    [InlineData("go/promo", "go%2Fpromo")]
    public async Task Get_EncodesCodeInPath(string code, string expectedSegment)
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.GetAsync(code);

        var raw = handler.Requests[0].Uri.AbsoluteUri;
        Assert.Contains($"/v1/links/{expectedSegment}", raw, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Stats_EncodesCodeWithSlashInPath()
    {
        var handler = StubHttpMessageHandler.Json("""
            {"code":"go/promo","days":30,"total_clicks":0,"qr_scans":0,"countries":[],"referrers":[]}
            """);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.StatsAsync("go/promo");

        Assert.Contains("/v1/links/go%2Fpromo/stats", handler.Requests[0].Uri.AbsoluteUri, StringComparison.Ordinal);
    }
}
