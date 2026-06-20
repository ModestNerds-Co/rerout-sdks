using System;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using Rerout;
using Rerout.Models;
using Xunit;

namespace Rerout.Tests;

public sealed class TagsTests
{
    private const string SampleTagJson = """
        {"id":"tag_abc123","name":"Spring 2026","color":"teal"}
        """;

    [Fact]
    public async Task List_GetsTagsEndpoint_ParsesLinkCount()
    {
        var handler = StubHttpMessageHandler.Json("""
            {"tags":[{"id":"tag_abc123","name":"Spring 2026","color":"teal","link_count":4}]}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Tags.ListAsync();

        Assert.Single(result.Tags);
        Assert.Equal("tag_abc123", result.Tags[0].Id);
        Assert.Equal("Spring 2026", result.Tags[0].Name);
        Assert.Equal("teal", result.Tags[0].Color);
        Assert.Equal(4, result.Tags[0].LinkCount);

        Assert.Equal(HttpMethod.Get, handler.Requests[0].Method);
        Assert.EndsWith("/v1/projects/me/tags", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);
        // GET has no request body.
        Assert.True(string.IsNullOrEmpty(handler.Requests[0].Body));
    }

    [Fact]
    public async Task List_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.Unauthorized, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Tags.ListAsync());

        Assert.Equal("unauthorized", ex.Code);
    }

    [Fact]
    public async Task Create_PostsNameAndColor_ReturnsTag()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.Created, SampleTagJson);
        using var client = TestHelpers.ClientWith(handler);

        var tag = await client.Tags.CreateAsync(new CreateTagInput
        {
            Name = "Spring 2026",
            Color = "teal",
        });

        Assert.Equal("tag_abc123", tag.Id);
        Assert.Equal("Spring 2026", tag.Name);
        Assert.Equal("teal", tag.Color);

        Assert.Equal(HttpMethod.Post, handler.Requests[0].Method);
        Assert.EndsWith("/v1/projects/me/tags", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("Spring 2026", root.GetProperty("name").GetString());
        Assert.Equal("teal", root.GetProperty("color").GetString());
    }

    [Fact]
    public async Task Create_OmitsColor_WhenNotProvided()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.Created, SampleTagJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Tags.CreateAsync(new CreateTagInput { Name = "Spring 2026" });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("Spring 2026", root.GetProperty("name").GetString());
        Assert.False(root.TryGetProperty("color", out _));
    }

    [Fact]
    public async Task Create_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(
            HttpStatusCode.BadRequest,
            """{"code":"bad_tag_color","message":"unknown color."}""");
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Tags.CreateAsync(new CreateTagInput { Name = "x", Color = "chartreuse" }));

        Assert.Equal("bad_tag_color", ex.Code);
    }

    [Fact]
    public async Task Update_PatchesTagById_ForwardsOnlyColor()
    {
        var handler = StubHttpMessageHandler.Json("""
            {"id":"tag_abc123","name":"Spring 2026","color":"red"}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var tag = await client.Tags.UpdateAsync("tag_abc123", new UpdateTagInput { Color = "red" });

        Assert.Equal("red", tag.Color);

        Assert.Equal(HttpMethod.Patch, handler.Requests[0].Method);
        Assert.EndsWith("/v1/projects/me/tags/tag_abc123", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("red", root.GetProperty("color").GetString());
        Assert.False(root.TryGetProperty("name", out _));
    }

    [Fact]
    public async Task Update_ForwardsOnlyName_WhenOnlyNameProvided()
    {
        var handler = StubHttpMessageHandler.Json("""
            {"id":"tag_abc123","name":"Renamed","color":"teal"}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var tag = await client.Tags.UpdateAsync("tag_abc123", new UpdateTagInput { Name = "Renamed" });

        Assert.Equal("Renamed", tag.Name);

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("Renamed", root.GetProperty("name").GetString());
        Assert.False(root.TryGetProperty("color", out _));
    }

    [Fact]
    public async Task Update_EmptyInput_ThrowsBeforeHittingApi()
    {
        var handler = StubHttpMessageHandler.Json(SampleTagJson);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Tags.UpdateAsync("tag_abc123", new UpdateTagInput()));

        Assert.Equal("empty_update", ex.Code);
        Assert.Empty(handler.Requests);
    }

    [Fact]
    public async Task Update_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.NotFound, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Tags.UpdateAsync("tag_missing", new UpdateTagInput { Name = "x" }));

        Assert.Equal("not_found", ex.Code);
    }

    [Fact]
    public async Task Delete_SendsDelete_ReturnsDeleteResult()
    {
        var handler = StubHttpMessageHandler.Json("""{"deleted":true}""");
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Tags.DeleteAsync("tag_abc123");

        Assert.True(result.Deleted);
        Assert.Equal(HttpMethod.Delete, handler.Requests[0].Method);
        Assert.EndsWith("/v1/projects/me/tags/tag_abc123", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Delete_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.Forbidden, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Tags.DeleteAsync("tag_abc123"));

        Assert.Equal("forbidden", ex.Code);
    }

    [Theory]
    [InlineData("tag_abc123", "tag_abc123")]
    [InlineData("tag a/b", "tag%20a%2Fb")]
    [InlineData("tag+x", "tag%2Bx")]
    [InlineData("café", "caf%C3%A9")]
    public async Task Delete_EncodesTagIdInPath(string tagId, string expectedSegment)
    {
        var handler = StubHttpMessageHandler.Json("""{"deleted":true}""");
        using var client = TestHelpers.ClientWith(handler);

        await client.Tags.DeleteAsync(tagId);

        var raw = handler.Requests[0].Uri.AbsoluteUri;
        Assert.Contains($"/v1/projects/me/tags/{expectedSegment}", raw, StringComparison.Ordinal);
    }

    [Theory]
    [InlineData("tag a/b", "tag%20a%2Fb")]
    [InlineData("café", "caf%C3%A9")]
    public async Task Update_EncodesTagIdInPath(string tagId, string expectedSegment)
    {
        var handler = StubHttpMessageHandler.Json(SampleTagJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Tags.UpdateAsync(tagId, new UpdateTagInput { Name = "x" });

        var raw = handler.Requests[0].Uri.AbsoluteUri;
        Assert.Contains($"/v1/projects/me/tags/{expectedSegment}", raw, StringComparison.Ordinal);
    }
}
