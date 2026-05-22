# Rerout SDKs

Official client libraries for the [Rerout](https://rerout.co) API — branded link infrastructure on the edge.

Each SDK is hand-written to feel native in its language. They all wrap the same surface:

- Create, list, update, and delete short links
- Read project and per-link analytics
- Build branded QR URLs
- Verify incoming webhook signatures

## Available SDKs

### Base SDKs

| Language        | Package                       | Path                            | Status         |
|-----------------|-------------------------------|---------------------------------|----------------|
| TypeScript / JS | `@rerout/sdk`                 | [`typescript/`](./typescript)   | v0.1 — preview |
| Dart            | `rerout`                      | [`dart/`](./dart)               | v0.1 — preview |
| Python          | `rerout`                      | [`python/`](./python)           | v0.1 — preview |
| PHP             | `rerout/sdk`                  | [`php/`](./php)                 | v0.1 — preview |
| Go              | `…/rerout-sdks/go`            | [`go/`](./go)                   | v0.1 — preview |
| Rust            | `rerout`                      | [`rust/`](./rust)               | v0.1 — preview |
| Ruby            | `rerout`                      | [`ruby/`](./ruby)               | v0.1 — preview |
| Kotlin / JVM    | `co.rerout:rerout-kotlin`     | [`kotlin/`](./kotlin)           | v0.1 — preview |
| C# / .NET       | `Rerout`                      | [`dotnet/`](./dotnet)           | v0.1 — preview |

### Framework integrations

| Framework      | Package                              | Path                                  | Status         |
|----------------|--------------------------------------|---------------------------------------|----------------|
| Django         | `rerout-django`                      | [`python-django/`](./python-django)   | v0.1 — preview |
| Laravel        | `rerout/laravel`                     | [`php-laravel/`](./php-laravel)        | v0.1 — preview |
| Rails          | `rerout-rails`                       | [`ruby-rails/`](./ruby-rails)          | v0.1 — preview |
| Spring Boot    | `co.rerout:rerout-spring-boot-starter` | [`kotlin-spring/`](./kotlin-spring) | v0.1 — preview |
| ASP.NET Core   | `Rerout.AspNetCore`                  | [`dotnet-aspnet/`](./dotnet-aspnet)    | v0.1 — preview |

Every SDK is hand-written, comprehensively tested, and published under the
[MIT License](#license). The shared spec all ports follow lives in
[`AGENTS.md`](./AGENTS.md).

## Quick start

### TypeScript / JavaScript

```bash
npm install @rerout/sdk
```

```ts
import { Rerout } from '@rerout/sdk'

const rerout = new Rerout({ apiKey: process.env.REROUT_API_KEY! })

const link = await rerout.links.create({
  target_url: 'https://example.com/q4-sale',
  domain_hostname: 'go.brand.com',
  code: 'q4',
})

console.log(link.short_url) // https://go.brand.com/q4
```

### Dart

```yaml
dependencies:
  rerout: ^0.1.0
```

```dart
import 'package:rerout/rerout.dart';

final rerout = Rerout.initialize(apiKey: 'rrk_...');

final result = await rerout.links.create(
  CreateLinkRequest(
    targetUrl: 'https://example.com/q4-sale',
    domainHostname: 'go.brand.com',
    code: 'q4',
  ),
);

switch (result) {
  case Success(:final data):
    print(data.shortUrl); // https://go.brand.com/q4
  case Error(:final error):
    print('Failed: ${error.message}');
}
```

## Authentication

Every SDK takes a project API key in the `rrk_…` format. Mint keys from your project's
**API keys** page in the Rerout dashboard. Treat them like passwords — never check them
into source, never ship them to a browser.

For browser-side use cases (e.g. real-time updates in a dashboard), use the cookie-based
session auth via `app.rerout.co` directly instead of an API key.

## API base URL

The default is `https://api.rerout.co`. Override per-client (useful for staging or
self-hosted setups):

```ts
new Rerout({ apiKey, baseUrl: 'https://api.staging.rerout.co' })
```

```dart
Rerout.initialize(apiKey: 'rrk_...', baseUrl: 'https://api.staging.rerout.co');
```

## Webhook signature verification

Incoming webhooks from Rerout are signed with HMAC-SHA256 using the endpoint's signing
secret. The header is `X-Rerout-Signature: t={unix},v1={hex_hmac}`. Each SDK ships a
verification helper that handles timestamp tolerance and constant-time comparison:

```ts
import { verifyReroutSignature } from '@rerout/sdk'

const ok = verifyReroutSignature({
  rawBody,
  signatureHeader: req.headers['x-rerout-signature'],
  secret: process.env.REROUT_WEBHOOK_SECRET!,
})
```

```dart
final ok = ReroutWebhookSignature.verify(
  rawBody: rawBody,
  signatureHeader: signatureHeader,
  secret: webhookSecret,
);
```

## Versioning

Each SDK follows semantic versioning independently. The API itself is versioned via the
`/v1` URL prefix; breaking API changes mean a new prefix, and SDKs roll forward
accordingly.

## Contributing

Each SDK directory has its own `README.md`, build instructions, and test suite. Drop into
the language directory you care about, hack, and PR. Maintainers see `MAINTAINERS.md` in
the language directory for release runbook.

## License

MIT. See [LICENSE](./LICENSE) in each SDK directory.

---

Built and maintained by [Codecraft Solutions](https://codecraftsolutions.co.za).
