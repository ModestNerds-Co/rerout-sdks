using System;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using Rerout;
using Rerout.Models;
using Xunit;

namespace Rerout.Tests;

public sealed class SmartLinksTests
{
    [Fact]
    public async Task Get_ParsesSmartLinksFields()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleSmartLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var link = await client.Links.GetAsync("vip");

        Assert.True(link.PasswordProtected);
        Assert.Equal(1000, link.MaxClicks);
        Assert.Equal(42, link.ClickCount);
        Assert.True(link.TrackConversions);

        Assert.Equal(2, link.RoutingRules.Count);
        Assert.Equal("country", link.RoutingRules[0].ConditionType);
        Assert.Equal("is", link.RoutingRules[0].ConditionOp);
        Assert.Equal("ZA", link.RoutingRules[0].ConditionValue);
        Assert.Equal("https://example.com/za", link.RoutingRules[0].TargetUrl);
        Assert.Equal("device", link.RoutingRules[1].ConditionType);
        Assert.Equal("in", link.RoutingRules[1].ConditionOp);

        Assert.Equal(2, link.AbVariants.Count);
        Assert.Equal(1, link.AbVariants[0].Id);
        Assert.Equal("https://example.com/a", link.AbVariants[0].TargetUrl);
        Assert.Equal(60, link.AbVariants[0].Weight);
    }

    [Fact]
    public async Task Get_MissingSmartLinksFields_DefaultsSensibly()
    {
        // The legacy SampleLinkJson has none of the smart-links keys.
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        var link = await client.Links.GetAsync("q4");

        Assert.False(link.PasswordProtected);
        Assert.Null(link.MaxClicks);
        Assert.Equal(0, link.ClickCount);
        Assert.False(link.TrackConversions);
        Assert.Empty(link.RoutingRules);
        Assert.Empty(link.AbVariants);
    }

    [Fact]
    public async Task Create_SerializesSmartLinksInput()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleSmartLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.CreateAsync(new CreateLinkInput
        {
            TargetUrl = "https://example.com/default",
            Password = "hunter2",
            MaxClicks = 1000,
            TrackConversions = true,
            RoutingRules =
            [
                new RoutingRule
                {
                    ConditionType = "country",
                    ConditionOp = "is",
                    ConditionValue = "ZA",
                    TargetUrl = "https://example.com/za",
                },
            ],
            AbVariants =
            [
                new AbVariantInput { TargetUrl = "https://example.com/a", Weight = 60 },
                new AbVariantInput { TargetUrl = "https://example.com/b" },
            ],
        });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("hunter2", root.GetProperty("password").GetString());
        Assert.Equal(1000, root.GetProperty("max_clicks").GetInt64());
        Assert.True(root.GetProperty("track_conversions").GetBoolean());

        var rule = root.GetProperty("routing_rules")[0];
        Assert.Equal("country", rule.GetProperty("condition_type").GetString());
        Assert.Equal("is", rule.GetProperty("condition_op").GetString());
        Assert.Equal("ZA", rule.GetProperty("condition_value").GetString());
        Assert.Equal("https://example.com/za", rule.GetProperty("target_url").GetString());

        var variants = root.GetProperty("ab_variants");
        Assert.Equal(2, variants.GetArrayLength());
        Assert.Equal(60, variants[0].GetProperty("weight").GetInt32());
        Assert.Equal("https://example.com/b", variants[1].GetProperty("target_url").GetString());
        // The variant with no weight omits the field.
        Assert.False(variants[1].TryGetProperty("weight", out _));
    }

    [Fact]
    public async Task Create_OmitsSmartLinksFields_WhenNotSet()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.CreateAsync(new CreateLinkInput { TargetUrl = "https://example.com" });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.False(root.TryGetProperty("password", out _));
        Assert.False(root.TryGetProperty("max_clicks", out _));
        Assert.False(root.TryGetProperty("track_conversions", out _));
        Assert.False(root.TryGetProperty("routing_rules", out _));
        Assert.False(root.TryGetProperty("ab_variants", out _));
    }

    [Fact]
    public async Task Update_SerializesRoutingRulesAndAbVariants_AsFullReplace()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleSmartLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.UpdateAsync("vip", new UpdateLinkInput
        {
            TrackConversions = Optional<bool>.Set(true),
            RoutingRules = Optional<System.Collections.Generic.IReadOnlyList<RoutingRule>>.Set(
            [
                new RoutingRule
                {
                    ConditionType = "device",
                    ConditionOp = "is_not",
                    ConditionValue = "bot",
                    TargetUrl = "https://example.com/human",
                },
            ]),
            AbVariants = Optional<System.Collections.Generic.IReadOnlyList<AbVariantInput>>.Set(
            [
                new AbVariantInput { TargetUrl = "https://example.com/v2", Weight = 100 },
            ]),
        });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.True(root.GetProperty("track_conversions").GetBoolean());
        Assert.Equal("is_not", root.GetProperty("routing_rules")[0].GetProperty("condition_op").GetString());
        Assert.Equal(100, root.GetProperty("ab_variants")[0].GetProperty("weight").GetInt32());
    }

    [Fact]
    public async Task Update_SetsPasswordAndMaxClicks()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleSmartLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.UpdateAsync("vip", new UpdateLinkInput
        {
            Password = Optional<string?>.Set("s3cret"),
            MaxClicks = Optional<long?>.Set(500),
        });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("s3cret", root.GetProperty("password").GetString());
        Assert.Equal(500, root.GetProperty("max_clicks").GetInt64());
    }

    [Fact]
    public async Task Update_ClearsPasswordAndMaxClicks_WithExplicitNull()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleSmartLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.UpdateAsync("vip", new UpdateLinkInput
        {
            Password = Optional<string?>.Set(null),
            MaxClicks = Optional<long?>.Set(null),
        });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal(JsonValueKind.Null, root.GetProperty("password").ValueKind);
        Assert.Equal(JsonValueKind.Null, root.GetProperty("max_clicks").ValueKind);
    }

    [Fact]
    public async Task Update_ClearsRoutingRules_WithEmptyList()
    {
        var handler = StubHttpMessageHandler.Json(TestHelpers.SampleSmartLinkJson);
        using var client = TestHelpers.ClientWith(handler);

        await client.Links.UpdateAsync("vip", new UpdateLinkInput
        {
            RoutingRules = Optional<System.Collections.Generic.IReadOnlyList<RoutingRule>>.Set([]),
        });

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var rules = doc.RootElement.GetProperty("routing_rules");
        Assert.Equal(JsonValueKind.Array, rules.ValueKind);
        Assert.Equal(0, rules.GetArrayLength());
    }

    [Fact]
    public async Task CreateBatch_PostsWrappedLinksBody_ReturnsResults()
    {
        var handler = StubHttpMessageHandler.Json("""
            {
              "created": 2,
              "total": 3,
              "results": [
                {"index": 0, "ok": true, "code": "a1"},
                {"index": 1, "ok": true, "code": "b2"},
                {"index": 2, "ok": false, "error": "bad_target_url"}
              ]
            }
            """);
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Links.CreateBatchAsync(
        [
            new BatchLinkInput { TargetUrl = "https://example.com/1" },
            new BatchLinkInput { TargetUrl = "https://example.com/2", Code = "b2" },
            new BatchLinkInput { TargetUrl = "nope" },
        ]);

        Assert.Equal(HttpMethod.Post, handler.Requests[0].Method);
        Assert.EndsWith("/v1/links/batch", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var links = doc.RootElement.GetProperty("links");
        Assert.Equal(3, links.GetArrayLength());
        Assert.Equal("https://example.com/1", links[0].GetProperty("target_url").GetString());
        Assert.Equal("b2", links[1].GetProperty("code").GetString());

        Assert.Equal(2, result.Created);
        Assert.Equal(3, result.Total);
        Assert.Equal(3, result.Results.Count);
        Assert.Equal(0, result.Results[0].Index);
        Assert.True(result.Results[0].Ok);
        Assert.Equal("a1", result.Results[0].Code);
        Assert.False(result.Results[2].Ok);
        Assert.Equal("bad_target_url", result.Results[2].Error);
    }

    [Fact]
    public async Task CreateBatch_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(
            (HttpStatusCode)429,
            """{"code":"rate_limited","message":"slow down"}""");
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Links.CreateBatchAsync(
            [
                new BatchLinkInput { TargetUrl = "https://example.com" },
            ]));

        Assert.Equal("rate_limited", ex.Code);
        Assert.True(ex.IsRateLimited);
    }
}
