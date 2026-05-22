using System;
using System.Net;
using System.Text.Json;
using System.Threading.Tasks;
using Rerout;
using Rerout.Models;
using Xunit;

namespace Rerout.Tests;

public sealed class TransportTests
{
    [Fact]
    public async Task Request_SendsBearerAuthHeader()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.GetAsync("q4");

        Assert.Single(handler.Requests);
        Assert.Equal($"Bearer {TestHelpers.ApiKey}", handler.Requests[0].Headers["Authorization"]);
    }

    [Fact]
    public async Task Request_SendsAcceptJsonHeader()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.GetAsync("q4");

        Assert.Contains("application/json", handler.Requests[0].Headers["Accept"], StringComparison.Ordinal);
    }

    [Fact]
    public async Task Request_WithBody_SetsContentTypeJson()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.CreateAsync(new CreateLinkInput { TargetUrl = "https://example.com" });

        Assert.Equal("application/json", handler.Requests[0].ContentType);
    }

    [Fact]
    public async Task Request_WithoutBody_SendsNoContentType()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.GetAsync("q4");

        Assert.Null(handler.Requests[0].ContentType);
        Assert.Null(handler.Requests[0].Body);
    }

    [Fact]
    public async Task Request_SerializesBodyAsSnakeCaseJson()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.CreateAsync(new CreateLinkInput
        {
            TargetUrl = "https://example.com/sale",
            DomainHostname = "go.brand.com",
            SeoTitle = "Hello",
        });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("https://example.com/sale", root.GetProperty("target_url").GetString());
        Assert.Equal("go.brand.com", root.GetProperty("domain_hostname").GetString());
        Assert.Equal("Hello", root.GetProperty("seo_title").GetString());
    }

    [Fact]
    public async Task Request_OmitsNullOptionalFieldsFromBody()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.CreateAsync(new CreateLinkInput { TargetUrl = "https://example.com" });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        Assert.False(doc.RootElement.TryGetProperty("domain_hostname", out _));
        Assert.False(doc.RootElement.TryGetProperty("seo_title", out _));
    }

    [Fact]
    public async Task Request_EmitsCursorAndLimitQueryParams()
    {
        var handler = StubHttpMessageHandler.Json("""{"links":[],"next_cursor":null}""");
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.ListAsync(cursor: 42, limit: 10);

        var query = handler.Requests[0].Uri.Query;
        Assert.Contains("cursor=42", query, StringComparison.Ordinal);
        Assert.Contains("limit=10", query, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Request_OmitsQueryParamsWhenNotSupplied()
    {
        var handler = StubHttpMessageHandler.Json("""{"links":[],"next_cursor":null}""");
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.ListAsync();

        Assert.Equal(string.Empty, handler.Requests[0].Uri.Query);
    }

    [Fact]
    public async Task Request_EmitsDaysQueryParam()
    {
        var handler = StubHttpMessageHandler.Json("""
            {"days":7,"total_clicks":0,"qr_scans":0,"daily":[],"countries":[],
             "referrers":[],"devices":[],"browsers":[],"top_codes":[]}
            """);
        using var client = TestHelpers.ClientWith(handler);

        await client.Project.StatsAsync(days: 7);

        Assert.Contains("days=7", handler.Requests[0].Uri.Query, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Error_ParsesServerCodeAndMessage()
    {
        var handler = StubHttpMessageHandler.Always(
            HttpStatusCode.BadRequest,
            """{"code":"bad_target_url","message":"target_url must use https.","timestamp":"2026-05-22T00:00:00Z"}""");
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Links.CreateAsync(new CreateLinkInput { TargetUrl = "http://insecure" }));

        Assert.Equal("bad_target_url", ex.Code);
        Assert.Equal("target_url must use https.", ex.Message);
        Assert.Equal(400, ex.Status);
        Assert.Equal("2026-05-22T00:00:00Z", ex.Timestamp);
    }

    [Theory]
    [InlineData(HttpStatusCode.Unauthorized, "unauthorized", 401)]
    [InlineData(HttpStatusCode.Forbidden, "forbidden", 403)]
    [InlineData(HttpStatusCode.NotFound, "not_found", 404)]
    [InlineData(HttpStatusCode.TooManyRequests, "rate_limited", 429)]
    [InlineData(HttpStatusCode.InternalServerError, "server_error", 500)]
    [InlineData(HttpStatusCode.BadGateway, "server_error", 502)]
    [InlineData(HttpStatusCode.BadRequest, "client_error", 400)]
    public async Task Error_SyntheticCodeForStatusWithNoBody(
        HttpStatusCode status,
        string expectedCode,
        int expectedStatus)
    {
        var handler = StubHttpMessageHandler.Always(status, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.GetAsync("q4"));

        Assert.Equal(expectedCode, ex.Code);
        Assert.Equal(expectedStatus, ex.Status);
    }

    [Fact]
    public async Task Error_RateLimitedFlagIsSet()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.TooManyRequests, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.GetAsync("q4"));

        Assert.True(ex.IsRateLimited);
        Assert.False(ex.IsServerError);
    }

    [Fact]
    public async Task Error_ServerErrorFlagIsSet()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.ServiceUnavailable, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.GetAsync("q4"));

        Assert.True(ex.IsServerError);
        Assert.False(ex.IsRateLimited);
    }

    [Fact]
    public async Task Error_NetworkFailureMapsToNetworkError()
    {
        var handler = StubHttpMessageHandler.NetworkFailure("dns lookup failed");
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.GetAsync("q4"));

        Assert.Equal("network_error", ex.Code);
        Assert.Equal(0, ex.Status);
    }

    [Fact]
    public async Task Error_TimeoutMapsToTimeout()
    {
        var handler = StubHttpMessageHandler.Hang();
        using var client = TestHelpers.ClientWith(handler);

        // The client owns timeout enforcement; shrink it for the test.
        using var http = new System.Net.Http.HttpClient(handler);
        using var fastClient = new ReroutClient(TestHelpers.ApiKey, new ReroutClientOptions
        {
            HttpClient = http,
            Timeout = TimeSpan.FromMilliseconds(100),
        });

        var ex = await Assert.ThrowsAsync<ReroutException>(() => fastClient.Links.GetAsync("q4"));

        Assert.Equal("timeout", ex.Code);
        Assert.Equal(0, ex.Status);
    }

    [Fact]
    public async Task Error_NonJsonSuccessBodyMapsToUnexpectedResponse()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.OK, "<html>not json</html>", "text/html");
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.GetAsync("q4"));

        Assert.Equal("unexpected_response", ex.Code);
        Assert.Equal(200, ex.Status);
    }

    [Fact]
    public async Task Error_NonJsonErrorBodyFallsBackToSyntheticCode()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.ServiceUnavailable, "upstream timeout", "text/plain");
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.GetAsync("q4"));

        Assert.Equal("server_error", ex.Code);
        Assert.Equal(503, ex.Status);
    }

    [Fact]
    public async Task Error_ToStringIncludesCodeAndStatus()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.NotFound, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Links.GetAsync("missing"));

        Assert.Contains("not_found", ex.ToString(), StringComparison.Ordinal);
        Assert.Contains("404", ex.ToString(), StringComparison.Ordinal);
    }
}
