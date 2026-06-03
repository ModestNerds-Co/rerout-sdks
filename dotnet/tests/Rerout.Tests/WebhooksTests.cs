using System;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using Rerout;
using Rerout.Models;
using Xunit;

namespace Rerout.Tests;

public sealed class WebhooksTests
{
    private const string SampleWebhookJson = """
        {
          "id": "wh_abc123",
          "project_id": "prj_test",
          "name": "Order events",
          "url": "https://example.com/hooks/rerout",
          "events": ["link.created", "link.clicked"],
          "is_active": true,
          "payload_format": "json",
          "created_at": 1700000000,
          "updated_at": 1700000000,
          "last_delivery_at": null,
          "last_success_at": null,
          "last_failure_at": null
        }
        """;

    [Fact]
    public async Task Create_PostsToWebhooksEndpoint_ReturnsSigningSecretAndEndpoint()
    {
        var handler = StubHttpMessageHandler.Always(
            HttpStatusCode.Created,
            $$"""
            {"endpoint":{{SampleWebhookJson}},"signing_secret":"whsec_supersecret"}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Webhooks.CreateAsync(new CreateWebhookInput
        {
            Name = "Order events",
            Url = "https://example.com/hooks/rerout",
            Events = ["link.created", "link.clicked"],
        });

        Assert.Equal("whsec_supersecret", result.SigningSecret);
        Assert.Equal("wh_abc123", result.Endpoint.Id);
        Assert.Equal("prj_test", result.Endpoint.ProjectId);
        Assert.Equal("json", result.Endpoint.PayloadFormat);
        Assert.True(result.Endpoint.IsActive);
        Assert.Equal(2, result.Endpoint.Events.Count);
        Assert.Null(result.Endpoint.LastDeliveryAt);

        Assert.Equal(HttpMethod.Post, handler.Requests[0].Method);
        Assert.EndsWith("/v1/projects/me/webhooks", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.Equal("Order events", root.GetProperty("name").GetString());
        Assert.Equal("https://example.com/hooks/rerout", root.GetProperty("url").GetString());
        Assert.Equal(2, root.GetProperty("events").GetArrayLength());
        // Optional fields omitted when not provided.
        Assert.False(root.TryGetProperty("is_active", out _));
        Assert.False(root.TryGetProperty("payload_format", out _));
    }

    [Fact]
    public async Task Create_ForwardsIsActiveAndPayloadFormat_WhenProvided()
    {
        var handler = StubHttpMessageHandler.Always(
            HttpStatusCode.Created,
            """
            {"endpoint":{"id":"wh_x","project_id":"prj_test","name":"Slack","url":"https://hooks.slack.com/services/T/B/x","events":["link.created"],"is_active":false,"payload_format":"slack","created_at":1700000000,"updated_at":1700000000,"last_delivery_at":null,"last_success_at":null,"last_failure_at":null},"signing_secret":"whsec_x"}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Webhooks.CreateAsync(new CreateWebhookInput
        {
            Name = "Slack",
            Url = "https://hooks.slack.com/services/T/B/x",
            Events = ["link.created"],
            IsActive = false,
            PayloadFormat = "slack",
        });

        Assert.Equal("slack", result.Endpoint.PayloadFormat);
        Assert.False(result.Endpoint.IsActive);

        using var doc = JsonDocument.Parse(handler.Requests[0].Body!);
        var root = doc.RootElement;
        Assert.False(root.GetProperty("is_active").GetBoolean());
        Assert.Equal("slack", root.GetProperty("payload_format").GetString());
    }

    [Fact]
    public async Task Create_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(
            HttpStatusCode.BadRequest,
            """{"code":"bad_webhook_url","message":"https only."}""");
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(
            () => client.Webhooks.CreateAsync(new CreateWebhookInput
            {
                Name = "x",
                Url = "http://x",
                Events = ["link.created"],
            }));

        Assert.Equal("bad_webhook_url", ex.Code);
    }

    [Fact]
    public async Task List_GetsWebhooksEndpoint_ParsesEndpointsAndEventTypes()
    {
        var handler = StubHttpMessageHandler.Json($$"""
            {"endpoints":[{{SampleWebhookJson}}],"event_types":["link.created","link.clicked","domain.verified"]}
            """);
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Webhooks.ListAsync();

        Assert.Single(result.Endpoints);
        Assert.Equal("https://example.com/hooks/rerout", result.Endpoints[0].Url);
        Assert.Equal(3, result.EventTypes.Count);
        Assert.Contains("domain.verified", result.EventTypes);

        Assert.Equal(HttpMethod.Get, handler.Requests[0].Method);
        Assert.EndsWith("/v1/projects/me/webhooks", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);
    }

    [Fact]
    public async Task List_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.Unauthorized, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Webhooks.ListAsync());

        Assert.Equal("unauthorized", ex.Code);
    }

    [Fact]
    public async Task Delete_SendsDelete_ReturnsDeleteResult()
    {
        var handler = StubHttpMessageHandler.Json("""{"deleted":true}""");
        using var client = TestHelpers.ClientWith(handler);

        var result = await client.Webhooks.DeleteAsync("wh_abc123");

        Assert.True(result.Deleted);
        Assert.Equal(HttpMethod.Delete, handler.Requests[0].Method);
        Assert.EndsWith("/v1/projects/me/webhooks/wh_abc123", handler.Requests[0].Uri.AbsolutePath, StringComparison.Ordinal);
    }

    [Fact]
    public async Task Delete_Error_ThrowsReroutException()
    {
        var handler = StubHttpMessageHandler.Always(HttpStatusCode.Forbidden, string.Empty);
        using var client = TestHelpers.ClientWith(handler);

        var ex = await Assert.ThrowsAsync<ReroutException>(() => client.Webhooks.DeleteAsync("wh_abc123"));

        Assert.Equal("forbidden", ex.Code);
    }

    [Theory]
    [InlineData("wh_abc123", "wh_abc123")]
    [InlineData("wh a/b", "wh%20a%2Fb")]
    [InlineData("wh+x", "wh%2Bx")]
    public async Task Delete_EncodesEndpointIdInPath(string endpointId, string expectedSegment)
    {
        var handler = StubHttpMessageHandler.Json("""{"deleted":true}""");
        using var client = TestHelpers.ClientWith(handler);

        await client.Webhooks.DeleteAsync(endpointId);

        var raw = handler.Requests[0].Uri.AbsoluteUri;
        Assert.Contains($"/v1/projects/me/webhooks/{expectedSegment}", raw, StringComparison.Ordinal);
    }
}
