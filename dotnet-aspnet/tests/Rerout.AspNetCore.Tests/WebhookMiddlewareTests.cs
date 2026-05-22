using System;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Rerout.AspNetCore;
using Rerout.AspNetCore.Events;
using Xunit;

namespace Rerout.AspNetCore.Tests;

public sealed class WebhookMiddlewareTests
{
    private const string Secret = "whsec_test_secret";

    private const string LinkClickedBody = """
        {
          "id": "evt_123",
          "type": "link.clicked",
          "created_at": 1716200000,
          "data": {
            "code": "q4",
            "target_url": "https://example.com/q4-sale",
            "domain_hostname": "go.brand.com",
            "country": "ZA",
            "referrer": "twitter.com",
            "is_qr": false,
            "clicked_at": 1716200000
          }
        }
        """;

    private static HttpRequestMessage Post(string body, string? signature)
    {
        var request = new HttpRequestMessage(HttpMethod.Post, "/webhooks/rerout")
        {
            Content = new StringContent(body, Encoding.UTF8, "application/json"),
        };
        if (signature is not null)
        {
            request.Headers.Add(ReroutWebhookMiddleware.SignatureHeaderName, signature);
        }

        return request;
    }

    [Fact]
    public async Task ValidSignature_Returns200_AndDispatchesEvent()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(LinkClickedBody, Secret);
        var response = await client.SendAsync(Post(LinkClickedBody, signature));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Single(factory.Recorder.Received);
        Assert.Equal("link.clicked", factory.Recorder.Received[0].Type);
        Assert.Equal("evt_123", factory.Recorder.Received[0].Id);
        Assert.Equal(1716200000, factory.Recorder.Received[0].CreatedAt);
    }

    [Fact]
    public async Task ValidSignature_EventDeserializesToTypedRecord()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(LinkClickedBody, Secret);
        await client.SendAsync(Post(LinkClickedBody, signature));

        var data = factory.Recorder.Received[0].GetData<LinkClicked>();
        Assert.Equal("q4", data.Code);
        Assert.Equal("https://example.com/q4-sale", data.TargetUrl);
        Assert.Equal("ZA", data.Country);
        Assert.False(data.IsQr);
    }

    [Fact]
    public async Task WrongSecret_Returns401_AndDoesNotDispatch()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(LinkClickedBody, "whsec_wrong_secret");
        var response = await client.SendAsync(Post(LinkClickedBody, signature));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        Assert.Empty(factory.Recorder.Received);
    }

    [Fact]
    public async Task MissingSignatureHeader_Returns401()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var response = await client.SendAsync(Post(LinkClickedBody, signature: null));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        Assert.Empty(factory.Recorder.Received);
    }

    [Fact]
    public async Task TamperedBody_Returns401()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        // Sign the original, then send a modified body.
        var signature = WebhookSigning.Header(LinkClickedBody, Secret);
        var response = await client.SendAsync(Post(LinkClickedBody + " ", signature));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task ExpiredTimestamp_Returns401()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret).WithTolerance(300);
        using var client = factory.CreateClient();

        var staleTs = DateTimeOffset.UtcNow.ToUnixTimeSeconds() - 3600;
        var signature = WebhookSigning.Header(LinkClickedBody, Secret, staleTs);
        var response = await client.SendAsync(Post(LinkClickedBody, signature));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task ToleranceZero_AcceptsOldTimestamp()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret).WithTolerance(0);
        using var client = factory.CreateClient();

        var staleTs = DateTimeOffset.UtcNow.ToUnixTimeSeconds() - 999_999;
        var signature = WebhookSigning.Header(LinkClickedBody, Secret, staleTs);
        var response = await client.SendAsync(Post(LinkClickedBody, signature));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task MalformedSignatureHeader_Returns401()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var response = await client.SendAsync(Post(LinkClickedBody, "garbage"));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task ValidSignatureButInvalidJson_Returns400()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        const string body = "{not valid json";
        var signature = WebhookSigning.Header(body, Secret);
        var response = await client.SendAsync(Post(body, signature));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Empty(factory.Recorder.Received);
    }

    [Fact]
    public async Task ValidSignatureButMissingType_Returns400()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        const string body = """{"id":"evt_1","data":{"code":"q4"}}""";
        var signature = WebhookSigning.Header(body, Secret);
        var response = await client.SendAsync(Post(body, signature));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task ValidSignatureButMissingData_Returns400()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        const string body = """{"id":"evt_1","type":"link.clicked"}""";
        var signature = WebhookSigning.Header(body, Secret);
        var response = await client.SendAsync(Post(body, signature));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task ValidSignatureButEmptyBody_Returns400()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(string.Empty, Secret);
        var response = await client.SendAsync(Post(string.Empty, signature));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task NoSigningSecretConfigured_Returns401()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(string.Empty);
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(LinkClickedBody, Secret);
        var response = await client.SendAsync(Post(LinkClickedBody, signature));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task NoHandlerRegistered_Returns500()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret).WithoutHandler();
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(LinkClickedBody, Secret);
        var response = await client.SendAsync(Post(LinkClickedBody, signature));

        Assert.Equal(HttpStatusCode.InternalServerError, response.StatusCode);
    }

    [Fact]
    public async Task QrScannedEvent_DeserializesToTypedRecord()
    {
        const string body = """
            {
              "id": "evt_qr",
              "type": "qr.scanned",
              "created_at": 1716200500,
              "data": {
                "code": "menu",
                "target_url": "https://example.com/menu",
                "domain_hostname": null,
                "country": "US",
                "scanned_at": 1716200500
              }
            }
            """;
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(body, Secret);
        var response = await client.SendAsync(Post(body, signature));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var data = factory.Recorder.Received[0].GetData<QrScanned>();
        Assert.Equal("menu", data.Code);
        Assert.Equal("US", data.Country);
        Assert.Equal(1716200500, data.ScannedAt);
    }

    [Fact]
    public async Task DomainFailedEvent_DeserializesToTypedRecord()
    {
        const string body = """
            {
              "id": "evt_dom",
              "type": "domain.failed",
              "created_at": 1716201000,
              "data": {
                "hostname": "go.brand.com",
                "reason": "dns_unverified",
                "message": "CNAME record not found.",
                "failed_at": 1716201000
              }
            }
            """;
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(body, Secret);
        var response = await client.SendAsync(Post(body, signature));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var data = factory.Recorder.Received[0].GetData<DomainFailed>();
        Assert.Equal("go.brand.com", data.Hostname);
        Assert.Equal("dns_unverified", data.Reason);
        Assert.Equal("CNAME record not found.", data.Message);
    }

    [Fact]
    public async Task HandlerThrows_BubblesAsServerError()
    {
        await using var factory = new ReroutWebhookFactory()
            .WithSecret(Secret)
            .ConfigureHandler(h => h.ThrowOnHandle = new InvalidOperationException("handler boom"));
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(LinkClickedBody, Secret);
        var response = await client.SendAsync(Post(LinkClickedBody, signature));

        Assert.Equal(HttpStatusCode.InternalServerError, response.StatusCode);
    }

    [Fact]
    public async Task CasingVariationsInSignatureHeader_Accepted()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var ts = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var v1 = WebhookSigning.Sign(LinkClickedBody, ts, Secret);
        var header = $"T={ts},V1={v1}";
        var response = await client.SendAsync(Post(LinkClickedBody, header));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task EventRawDataIsAccessibleAsJsonElement()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var signature = WebhookSigning.Header(LinkClickedBody, Secret);
        await client.SendAsync(Post(LinkClickedBody, signature));

        var element = factory.Recorder.Received[0].Data;
        Assert.Equal("q4", element.GetProperty("code").GetString());
    }

    [Fact]
    public async Task GetRoute_StillWorksAlongsideWebhook()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var response = await client.GetAsync("/health");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task WebhookEndpoint_RejectsGetRequests()
    {
        await using var factory = new ReroutWebhookFactory().WithSecret(Secret);
        using var client = factory.CreateClient();

        var response = await client.GetAsync("/webhooks/rerout");

        Assert.Equal(HttpStatusCode.MethodNotAllowed, response.StatusCode);
    }
}
