# Changelog

All notable changes to `rerout/sdk` are documented in this file. The format is
based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-06-03

### Added

- Webhook endpoint management via a new `webhooks()` namespace — `create`,
  `list`, and `delete` against `/v1/projects/me/webhooks` (API-key auth). The
  signing secret returned by `create` is shown once.

## [0.2.0] - 2026-06-02

### Added

- Read-only `tags` field on `Link` — a list of `Tag` objects (`id`, `name`,
  `color`). Populated on get/list/update responses, empty on create. Missing
  `tags` in a payload parses to an empty array. Tag writes are not supported for
  API-key clients.

### Notes

- The project stats endpoint `/v1/projects/me/stats` is now live; `project()->stats()`
  targets it.

## [0.1.0] - 2026-05-20

### Added

- Initial public release.
- `Rerout` client with `links()`, `project()`, and `qr()` namespaces, Guzzle 7
  transport, and an injectable HTTP client for testing.
- Link operations: `create`, `list`, `get`, `update`, `delete`, `stats`.
- Project operations: `stats`, `me`.
- QR helpers: pure `url()` builder + signed `svg()` fetch.
- `CreateLinkInput` / `UpdateLinkInput` request models — `UpdateLinkInput` uses
  `UNSET` / `CLEAR` sentinels to distinguish "leave alone" from "null on the
  server", and rejects empty payloads client-side.
- Typed response models: `Link`, `LinkStats`, `ListLinksResult`,
  `ProjectStats`, `DailyClicksPoint`, `StatsBreakdown`, `QrOptions`.
- `SignatureVerifier::verify` — HMAC-SHA256 webhook signature verification with
  configurable timestamp tolerance, injectable clock, and constant-time
  comparison.
- `ReroutException` with stable `code`, `status`, `path`, `timestamp`, and
  `details`; `isRateLimited()` and `isServerError()` convenience flags.
- Synthetic error codes (`network_error`, `timeout`, `unexpected_response`,
  `unauthorized`, `forbidden`, `not_found`, `rate_limited`, `server_error`,
  `client_error`) for responses without a JSON error body.

[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/php/v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/php-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/php-v0.1.0
