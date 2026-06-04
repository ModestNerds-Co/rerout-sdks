using System;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using Rerout;
using Rerout.Models;
using Xunit;

namespace Rerout.Tests;

public sealed class ConversionsTests
{
    [Fact]
    public async Task Record_PostsToConversionsEndpoint_WithBody()
    {
        var handler = StubHttpMessageHandler.Json("""{"recorded":true,"duplicate":false}""");
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Conversions.RecordAsync(new RecordConversionInput
        {
            ClickId = "rrid_123",
            EventName = "purchase",
            ValueCents = 1999,
            Currency = "USD",
        });

        Assert.Equal(HttpMethod.Post, handler.Requests[0].Method);
        Assert.EndsWith("/v1/conversions", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("rrid_123", root.GetProperty("click_id").GetString());
        Assert.Equal("purchase", root.GetProperty("event_name").GetString());
        Assert.Equal(1999, root.GetProperty("value_cents").GetInt64());
        Assert.Equal("USD", root.GetProperty("currency").GetString());

        Assert.True(result.Recorded);
        Assert.False(result.Duplicate);
    }

    [Fact]
    public async Task Record_OmitsOptionalFields_WhenNotSet()
    {
        var handler = StubHttpMessageHandler.Json("""{"recorded":true,"duplicate":false}""");
        using var client = TestHelpers.ClientWith(handler);

        await client.Conversions.RecordAsync(new RecordConversionInput
        {
            ClickId = "rrid_123",
            EventName = "signup",
        });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("rrid_123", root.GetProperty("click_id").GetString());
        Assert.Equal("signup", root.GetProperty("event_name").GetString());
        Assert.False(root.TryGetProperty("value_cents", out _));
        Assert.False(root.TryGetProperty("currency", out _));
    }

    [Fact]
    public async Task Record_SurfacesDuplicateFlag()
    {
        var handler = StubHttpMessageHandler.Json("""{"recorded":true,"duplicate":true}""");
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Conversions.RecordAsync(new RecordConversionInput
        {
            ClickId = "rrid_123",
            EventName = "purchase",
        });

        Assert.True(result.Recorded);
        Assert.True(result.Duplicate);
    }

    [Fact]
    public async Task Record_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(
            HttpStatusCode.NotFound,
            """{"code":"not_found","message":"no such click"}""");
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Conversions.RecordAsync(new RecordConversionInput
            {
                ClickId = "rrid_nope",
                EventName = "purchase",
            }));

        Assert.Equal("not_found", ex.Code);
        Assert.Equal(404, ex.Status);
    }
}
