# Changelog

All notable changes to the `Rerout.AspNetCore` package are documented in this
file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-05-22

### Added

- Initial public release.
- `AddRerout(apiKey)` — registers a singleton `ReroutClient` in DI; overloads
  accept explicit `ReroutClientOptions` or an `IServiceProvider`-based factory.
- `AddReroutWebhooks` — configures the webhook signing secret and timestamp
  tolerance; `AddReroutWebhookHandler<T>` registers the scoped event handler.
- `MapReroutWebhook(pattern)` — maps a `POST` endpoint that verifies the
  `X-Rerout-Signature` header, deserializes the event, dispatches it to
  `IReroutEventHandler`, and returns `200` / `401` / `400`.
- `ReroutWebhookMiddleware` — the verify-parse-dispatch pipeline behind the
  endpoint, also usable directly.
- `IReroutEventHandler` and `ReroutWebhookEvent` (with `Type`, `Id`,
  `CreatedAt`, raw `Data`, and typed `GetData<T>()`).
- Strongly-typed event records: `LinkClicked`, `QrScanned`, `DomainFailed`.
- Targets `net8.0`; depends on the base `Rerout` SDK.

[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/dotnet-aspnet-v0.1.0
