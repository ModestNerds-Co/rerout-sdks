# Changelog

All notable changes to the `co.rerout:rerout-kotlin` package are documented in
this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-06-03

### Added

- Webhook endpoint management via the `webhooks` namespace — `create`, `list`,
  and `delete` against `/v1/projects/me/webhooks` (API-key auth). The signing
  secret returned by `create` is shown once.

## [0.2.0] - 2026-06-02

### Added

- Read-only `tags` field on `Link` — a `List<Tag>` where each `Tag` carries
  `id`, `name`, and `color`. Populated on link reads (`get`, `list`) and after
  `create`/`update`; defaults to an empty list when the server omits it. Tags
  cannot be written through `create`/`update` for API-key clients.

### Notes

- The project stats endpoint `/v1/projects/me/stats` is now live; the existing
  `project.stats(...)` method targets it unchanged.

## [0.1.0] - 2026-05-22

### Added

- Initial public release.
- `Rerout` client with `links`, `project`, and `qr` namespaces. All network
  calls are `suspend` functions.
- Construction via `Rerout(apiKey = …)` with a default + overridable `baseUrl`
  and an injectable `OkHttpClient` / `Json` for tests and custom transports.
- Link operations: `create`, `list`, `get`, `update`, `delete`, `stats`.
- Project operations: `stats`, `me`.
- QR helpers: pure URL builder (`Rerout.qr.url`) and authenticated SVG fetch
  (`Rerout.qr.svg`).
- `UpdateLinkInput` with a fluent builder that distinguishes "leave unchanged"
  from "clear to null" via explicit `clear*` flags.
- `verifyReroutSignature` / `ReroutWebhooks.verifySignature` — HMAC-SHA256
  webhook signature verification with configurable timestamp tolerance and
  constant-time comparison.
- `ReroutException` with a stable `code`, `status`, `path`, `timestamp`, and
  `details`; `isRateLimited` / `isServerError` convenience flags.
- `@Serializable` data classes for every API shape (`Link`, `LinkStats`,
  `ProjectStats`, `StatsBreakdown`, `DailyClicksPoint`, …).
- `maven-publish` configuration targeting Maven Central as
  `co.rerout:rerout-kotlin`, with sources and javadoc jars.

[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/kotlin/v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/kotlin-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/kotlin-v0.1.0
