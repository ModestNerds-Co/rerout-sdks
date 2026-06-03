# Rerout

Official .NET SDK for the [Rerout](https://rerout.co) branded-link API.

Create short links, render QR codes, read analytics, and verify webhook
signatures.

## Install

```bash
dotnet add package Rerout
```

Targets `net8.0`. No third-party dependencies — built on `HttpClient` and
`System.Text.Json`.

## Usage

```csharp
using Rerout;
using Rerout.Models;

using var rerout = new ReroutClient("rrk_…");

var link = await rerout.Links.CreateAsync(new CreateLinkInput
{
    TargetUrl = "https://example.com/q4-sale",
    DomainHostname = "go.brand.com",
    Code = "q4",
});

Console.WriteLine(link.ShortUrl); // https://go.brand.com/q4

var stats = await rerout.Project.StatsAsync(days: 7);
Console.WriteLine($"Last 7 days: {stats.TotalClicks} clicks, {stats.QrScans} QR scans");
```

`ReroutClient` is thread-safe — construct it once and reuse it. It owns an
`HttpClient` unless you supply your own; `Dispose` it (or wrap it in `using`)
when the SDK created the client.

## API

### Construction

```csharp
// Minimal — production defaults.
using var rerout = new ReroutClient("rrk_…");

// With explicit options.
using var rerout = new ReroutClient("rrk_…", new ReroutClientOptions
{
    BaseUrl = "https://api.rerout.co", // optional — trailing slashes trimmed
    Timeout = TimeSpan.FromSeconds(30), // optional — per-request timeout
    HttpClient = myHttpClient,          // optional — bring your own HttpClient
});
```

A blank or missing API key throws `ReroutException` with code `missing_api_key`.

### Links

```csharp
await rerout.Links.CreateAsync(new CreateLinkInput { TargetUrl = "https://…" });
await rerout.Links.ListAsync(cursor: 0, limit: 50);
await rerout.Links.GetAsync("q4");
await rerout.Links.UpdateAsync("q4", new UpdateLinkInput { /* … */ });
await rerout.Links.DeleteAsync("q4");
await rerout.Links.StatsAsync("q4", days: 30);
```

Every `Link` carries a read-only `Tags` list of `Tag` records (`Id`, `Name`,
`Color`); it is empty when the link has no tags. Tags are assigned in the
Rerout dashboard — the API ignores tag writes from API-key clients.

`UpdateLinkInput` uses `Optional<T>` to distinguish three states:

```csharp
var update = new UpdateLinkInput
{
    IsActive = Optional<bool>.Set(false),       // change the value
    SeoTitle = Optional<string?>.Set(null),     // clear the field on the server
    // TargetUrl left at Optional<string>.Unset  → omitted from the request
};
await rerout.Links.UpdateAsync("q4", update);
```

An `UpdateLinkInput` with no fields set throws `ReroutException` with code
`empty_update` — it never reaches the API.

### Project

```csharp
await rerout.Project.StatsAsync(days: 30); // aggregate analytics
await rerout.Project.MeAsync();            // the project owning the API key
```

### QR codes

```csharp
// Pure URL builder — no network call.
string url = rerout.Qr.Url("q4", new QrOptions
{
    Size = 12,
    Margin = 2,
    Ecc = "H",
    Domain = "go.brand.com",
    Refresh = "v2",          // any token forces a fresh render
    // RefreshAlways = true  // alternatively emits refresh=1
});

// Authenticated fetch — returns the rendered SVG.
string svg = await rerout.Qr.SvgAsync("q4", new QrOptions { Size = 8 });
```

The QR endpoint is API-key authenticated, so a bare `<img>` tag cannot load it
directly — proxy the request server-side, or use `SvgAsync`.

### Webhook management

Register, list, and remove webhook endpoints for the project that owns the API
key. The project is resolved from the key, so no project id appears in the path.

```csharp
using Rerout.Models;

// Create an endpoint. The signing secret is returned once — store it now.
var created = await rerout.Webhooks.CreateAsync(new CreateWebhookInput
{
    Name = "Production",
    Url = "https://hooks.brand.com/rerout",
    Events = ["link.created", "link.clicked"],
    // IsActive = false,         // optional — defaults to active
    // PayloadFormat = "slack",  // optional — "json" (default) or "slack"
});

Console.WriteLine(created.Endpoint.Id);      // wh_…
Console.WriteLine(created.SigningSecret);    // whsec_… — shown only here

// List endpoints and every event type the server can deliver.
var list = await rerout.Webhooks.ListAsync();
foreach (var endpoint in list.Endpoints)
{
    Console.WriteLine($"{endpoint.Id} → {endpoint.Url}");
}

// Delete an endpoint by id. Idempotent.
await rerout.Webhooks.DeleteAsync(created.Endpoint.Id);
```

The `SigningSecret` from `CreateAsync` is shown once — persist it and use it
with `SignatureVerifier` (below) to verify inbound deliveries.

### Webhook signature verification

```csharp
using Rerout.Webhooks;

bool ok = SignatureVerifier.Verify(
    rawBody,
    request.Headers["X-Rerout-Signature"]!,
    Environment.GetEnvironmentVariable("REROUT_WEBHOOK_SECRET")!);

if (!ok)
{
    return Results.Unauthorized();
}
```

The header format is `t={unix},v1={hex_hmac_sha256}`. The HMAC-SHA256 is
computed over `"{timestamp}.{rawBody}"` and compared in constant time via
`CryptographicOperations.FixedTimeEquals`. Always verify against the **raw**,
unmodified request body.

Defaults to a 5-minute timestamp tolerance; pass `toleranceSeconds: 0` to
disable that staleness check.

Building an ASP.NET Core app? `Rerout.AspNetCore` wires the client into DI and
adds a signed-and-verified webhook endpoint.

## Error handling

Every method throws `ReroutException` on failure:

```csharp
using Rerout;

try
{
    await rerout.Links.CreateAsync(new CreateLinkInput { TargetUrl = "http://insecure" });
}
catch (ReroutException ex)
{
    Console.Error.WriteLine(ex.Code);    // "bad_target_url"
    Console.Error.WriteLine(ex.Status);  // 400
    Console.Error.WriteLine(ex.Message); // "target_url must use https."

    if (ex.IsRateLimited) { /* back off and retry */ }
    if (ex.IsServerError) { /* 5xx — server-side issue */ }
}
```

Synthetic `Code` values when the server didn't return a usable JSON body:
`network_error`, `timeout`, `unexpected_response`, `unauthorized`,
`forbidden`, `not_found`, `rate_limited`, `server_error`, `client_error`,
`missing_api_key`, `empty_update`.

## Local development

```bash
dotnet restore
dotnet build
dotnet test
dotnet format --verify-no-changes
```

## License

MIT — see [LICENSE](LICENSE). Part of the Rerout SDK workspace:
<https://github.com/ModestNerds-Co/rerout-sdks>.
