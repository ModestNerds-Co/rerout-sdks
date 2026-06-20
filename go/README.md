# rerout-go

Official Go SDK for the [Rerout](https://rerout.co) API — branded link
infrastructure on Cloudflare. Create short links, render QR codes, read
analytics, and verify webhook signatures.

## Install

```bash
go get github.com/ModestNerds-Co/rerout-sdks/go@latest
```

```go
import "github.com/ModestNerds-Co/rerout-sdks/go/rerout"
```

Requires Go 1.22+. No third-party dependencies — the SDK is built entirely on
the standard library.

## Usage

```go
package main

import (
	"context"
	"fmt"
	"log"
	"os"

	"github.com/ModestNerds-Co/rerout-sdks/go/rerout"
)

func main() {
	client, err := rerout.NewClient(os.Getenv("REROUT_API_KEY"))
	if err != nil {
		log.Fatal(err)
	}

	link, err := client.Links().Create(context.Background(), rerout.CreateLinkInput{
		TargetURL:      "https://example.com/q4-sale",
		DomainHostname: rerout.String("go.brand.com"),
		Code:           rerout.String("q4"),
	})
	if err != nil {
		log.Fatal(err)
	}

	fmt.Println(link.ShortURL) // https://go.brand.com/q4
}
```

## API

### Construction

`NewClient` takes the project API key plus functional options:

```go
client, err := rerout.NewClient("rrk_…",
	rerout.WithBaseURL("https://api.rerout.co"),         // optional, default shown
	rerout.WithTimeout(30*time.Second),                  // optional, default 30s
	rerout.WithHTTPClient(&http.Client{ /* … */ }),      // optional — inject your own
	rerout.WithDefaultHeaders(map[string]string{          // optional — added to every request
		"User-Agent": "my-app/1.0",
	}),
)
```

A blank API key returns a `*ReroutError` with code `missing_api_key`. Trailing
slashes on `baseURL` are trimmed. `WithTimeout` is ignored when
`WithHTTPClient` is also supplied — set the timeout on the injected client
instead. The SDK always owns the `Authorization`, `Accept`, and `Content-Type`
headers; default headers cannot override them.

Every request method takes a `context.Context` as its first argument, so
cancellation and deadlines work as expected.

### Links

```go
ctx := context.Background()

link, err := client.Links().Create(ctx, rerout.CreateLinkInput{
	TargetURL: "https://example.com/q4-sale",
})

page, err := client.Links().List(ctx, &rerout.ListLinksParams{
	Cursor: rerout.Int64(42),
	Limit:  rerout.Int(50),
})
// page.HasMore() reports whether page.NextCursor is set.

link, err = client.Links().Get(ctx, "q4")

link, err = client.Links().Update(ctx, "q4", rerout.UpdateLinkInput{
	TargetURL:      rerout.String("https://example.com/new"),
	ClearExpiresAt: true, // sends "expires_at": null
})

result, err := client.Links().Delete(ctx, "q4") // result.Deleted

stats, err := client.Links().Stats(ctx, "q4", 7) // days; <= 0 defaults to 30
```

`UpdateLinkInput` distinguishes three states per field:

- **leave untouched** — leave the field unset (`nil` pointer, no `Clear*` flag),
- **set a value** — assign a pointer via `rerout.String` / `Int64` / `Bool`,
- **clear server-side** — set the matching `ClearXxx` flag, which sends an
  explicit JSON `null`.

An `UpdateLinkInput` with no field set and no `Clear*` flag is rejected
client-side with code `bad_request` — it never hits the API.

Every `Link` carries a read-only `Tags` field — a slice of `Tag`
(`{ID, Name, Color}`) populated on `Get`, `List`, and `Update` (empty on
`Create`). Tags cannot be written through the API-key client.

### Project

```go
stats, err := client.Project().Stats(ctx, 30) // days; <= 0 defaults to 30
project, err := client.Project().Me(ctx)      // project.ID / .Name / .Slug
```

### QR codes

```go
// Pure URL builder — no network call.
url := client.QR().URL("q4", &rerout.QROptions{
	Size:        rerout.Int(12),
	Margin:      rerout.Int(2),
	ECC:         "H",            // "L", "M", "Q", or "H"
	Domain:      "go.brand.com",
	RefreshTrue: true,           // emits refresh=1; or set Refresh: "v2"
})

// Authenticated fetch — returns the rendered SVG as a string.
svg, err := client.QR().SVG(ctx, "q4", &rerout.QROptions{Size: rerout.Int(10)})
```

`rerout.BuildQRURL(baseURL, code, options)` is a standalone version of
`QR().URL` for when you have a base URL but not a full client.

### Webhook management

`client.Webhooks()` manages the project's webhook endpoints over the
`/v1/projects/me/webhooks` surface. The project is resolved from the API key,
so there is no project id in the path.

```go
ctx := context.Background()

created, err := client.Webhooks().Create(ctx, rerout.CreateWebhookInput{
	Name:          "Order events",
	URL:           "https://example.com/hooks/rerout",
	Events:        []string{"link.created", "link.clicked"},
	IsActive:      rerout.Bool(true),         // optional; server default is true
	PayloadFormat: rerout.String("json"),     // optional; server default is "json"
})
// created.Endpoint.ID is the "wh_…" endpoint id.
// created.SigningSecret ("whsec_…") is shown ONCE — persist it now to verify
// deliveries with VerifySignature; it is never returned again.

list, err := client.Webhooks().List(ctx)
// list.Endpoints is the registered endpoints; list.EventTypes lists every
// event type the server can deliver.

result, err := client.Webhooks().Delete(ctx, created.Endpoint.ID) // result.Deleted
```

`Name`, `URL`, and `Events` are required on `CreateWebhookInput`; the optional
pointer fields are omitted from the request when unset so the server applies
its defaults. `Delete` soft-deletes the endpoint and is idempotent.

This is distinct from the inbound signature helper below — `Webhooks()` manages
the endpoints you register, while `VerifySignature` validates the deliveries
the server sends to them.

### Tags

`client.Tags()` manages the project's tags over the `/v1/projects/me/tags`
surface. The project is resolved from the API key, so there is no project id in
the path.

```go
ctx := context.Background()

// List returns each tag with its live link count.
tags, err := client.Tags().List(ctx)
// tags.Tags[i].LinkCount is the number of non-deleted links the tag is on.

// Create — Name required; Color optional (server defaults it when omitted).
tag, err := client.Tags().Create(ctx, rerout.CreateTagInput{
	Name:  "Spring 2026",
	Color: rerout.String("teal"),
})

// Update — only the fields you set are sent; omitted fields are unchanged.
// An empty UpdateTagInput is rejected client-side and never hits the API.
tag, err = client.Tags().Update(ctx, tag.ID, rerout.UpdateTagInput{
	Color: rerout.String("red"),
})

result, err := client.Tags().Delete(ctx, tag.ID) // result.Deleted; drops link assignments
```

`Create`/`Update` return a plain `Tag` (`ID`, `Name`, `Color`); `List` returns
`TagSummary` values, which add `LinkCount`.

### Webhook signature verification

`VerifySignature` is a standalone helper — it needs no client:

```go
ok := rerout.VerifySignature(
	rawBody,                                // exact, unmodified request body
	r.Header.Get("X-Rerout-Signature"),     // "t=…,v1=…"
	os.Getenv("REROUT_WEBHOOK_SECRET"),     // whsec_…
)
if !ok {
	w.WriteHeader(http.StatusBadRequest)
	return
}
```

The header format is `t={unix},v1={hex_hmac_sha256}`; the HMAC is computed over
`"{timestamp}.{rawBody}"` and compared in constant time. The default timestamp
tolerance is 5 minutes — override it with `rerout.WithTolerance(seconds)`, or
pass `rerout.WithTolerance(0)` to disable the timestamp check entirely.
`rerout.WithClock(func() int64)` injects a clock for deterministic tests.

### Error handling

Every method returns `(T, error)`; on failure the error is always a
`*ReroutError`:

```go
_, err := client.Links().Create(ctx, rerout.CreateLinkInput{TargetURL: "http://insecure"})

var rerr *rerout.ReroutError
if errors.As(err, &rerr) {
	fmt.Println(rerr.Code)    // e.g. "bad_target_url"
	fmt.Println(rerr.Status)  // e.g. 400
	fmt.Println(rerr.Message) // human-readable
	if rerr.IsRateLimited() { /* back off and retry */ }
	if rerr.IsServerError() { /* 5xx — retry after backoff */ }
}
```

`rerout.AsReroutError(err)` is a convenience that returns the `*ReroutError` or
`nil`. `ReroutError` implements `Unwrap`, so `errors.Is` / `errors.As` reach
any wrapped cause.

Synthetic codes are used when the server did not return a JSON error body:
`unauthorized` (401), `forbidden` (403), `not_found` (404), `rate_limited`
(429), `server_error` (5xx), `client_error` (other 4xx), `network_error`
(connection failure), `timeout`, `unexpected_response` (2xx with a non-JSON
body), `missing_api_key`, and `bad_request` (client-side validation).

## Local development

```bash
go mod tidy
go vet ./...
go test -race ./...
gofmt -l .
```

## License

MIT — see [LICENSE](./LICENSE), a copy of the
[workspace LICENSE](../LICENSE).

## Repository

<https://github.com/ModestNerds-Co/rerout-sdks>
