# Rerout.AspNetCore

ASP.NET Core integration for the [Rerout](https://rerout.co) branded-link SDK.

Registers the Rerout client in dependency injection and adds a
signature-verified webhook endpoint — verify, deserialize, dispatch, in one
line of routing.

## Install

```bash
dotnet add package Rerout.AspNetCore
```

Targets `net8.0` and the ASP.NET Core shared framework. Pulls in the base
[`Rerout`](https://www.nuget.org/packages/Rerout) package.

## Register the client

```csharp
using Rerout.AspNetCore.DependencyInjection;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddRerout(builder.Configuration["Rerout:ApiKey"]!);
```

`AddRerout` registers a singleton `ReroutClient`. Inject it anywhere:

```csharp
app.MapGet("/links", async (ReroutClient rerout) =>
{
    var page = await rerout.Links.ListAsync(limit: 50);
    return page.Links;
});
```

Pass `ReroutClientOptions` for a custom base URL, timeout, or `HttpClient`:

```csharp
builder.Services.AddRerout(
    builder.Configuration["Rerout:ApiKey"]!,
    new ReroutClientOptions { Timeout = TimeSpan.FromSeconds(10) });
```

## Receive webhooks

Configure the signing secret, register a handler, and map the endpoint:

```csharp
using Rerout.AspNetCore;
using Rerout.AspNetCore.DependencyInjection;

builder.Services
    .AddRerout(builder.Configuration["Rerout:ApiKey"]!)
    .AddReroutWebhooks(builder.Configuration["Rerout:WebhookSecret"]!)
    .AddReroutWebhookHandler<ReroutEventHandler>();

var app = builder.Build();

app.MapReroutWebhook("/webhooks/rerout");

app.Run();
```

`MapReroutWebhook` maps a `POST` endpoint that:

1. reads the raw request body,
2. verifies the `X-Rerout-Signature` header (HMAC-SHA256, constant-time),
3. deserializes the event,
4. calls your `IReroutEventHandler`,
5. and returns the right status code.

| Outcome | Status |
|---|---|
| Verified, parsed, handled | `200 OK` |
| Missing or invalid signature | `401 Unauthorized` |
| Body absent or not a valid event | `400 Bad Request` |
| No handler registered / handler throws | `500` |

### Handle events

```csharp
using Rerout.AspNetCore;
using Rerout.AspNetCore.Events;

public sealed class ReroutEventHandler : IReroutEventHandler
{
    private readonly ILogger<ReroutEventHandler> _logger;

    public ReroutEventHandler(ILogger<ReroutEventHandler> logger) => _logger = logger;

    public Task HandleAsync(ReroutWebhookEvent webhookEvent, CancellationToken ct)
    {
        switch (webhookEvent.Type)
        {
            case "link.clicked":
                var click = webhookEvent.GetData<LinkClicked>();
                _logger.LogInformation("Click on {Code} from {Country}", click.Code, click.Country);
                break;

            case "qr.scanned":
                var scan = webhookEvent.GetData<QrScanned>();
                _logger.LogInformation("QR scan of {Code}", scan.Code);
                break;

            case "domain.failed":
                var failure = webhookEvent.GetData<DomainFailed>();
                _logger.LogWarning("Domain {Host} failed: {Reason}", failure.Hostname, failure.Reason);
                break;
        }

        return Task.CompletedTask;
    }
}
```

The handler is registered as **scoped**, so it may depend on scoped services
such as a `DbContext`. The middleware only invokes it after the signature has
been verified — the event is guaranteed authentic.

`ReroutWebhookEvent` exposes `Type`, `Id`, and `CreatedAt`, plus `Data` as a
`JsonElement` and a typed `GetData<T>()` for the records in
`Rerout.AspNetCore.Events`: `LinkClicked`, `QrScanned`, `DomainFailed`.

### Tolerance

`AddReroutWebhooks` defaults to a 5-minute timestamp tolerance. Override it:

```csharp
.AddReroutWebhooks(options =>
{
    options.SigningSecret = builder.Configuration["Rerout:WebhookSecret"]!;
    options.ToleranceSeconds = 0; // disable the replay-protection check
})
```

## Error handling

`ReroutClient` calls throw `ReroutException` on failure — see the base
[`Rerout`](https://www.nuget.org/packages/Rerout) package for `Code`, `Status`,
and the `IsRateLimited` / `IsServerError` flags.

## Local development

```bash
dotnet restore
dotnet build
dotnet test
dotnet format --verify-no-changes
```

The package references the base `Rerout` SDK by `ProjectReference` for local
development; published builds use a versioned `PackageReference`.

## License

MIT — see [LICENSE](LICENSE). Part of the Rerout SDK workspace:
<https://github.com/ModestNerds-Co/rerout-sdks>.
