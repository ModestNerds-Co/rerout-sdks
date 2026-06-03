# Changelog

All notable changes to the `rerout` gem are documented in this file. The
format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-06-03

### Added

- Webhook endpoint management via a new `webhooks` namespace — `create`, `list`,
  and `delete` against `/v1/projects/me/webhooks` (API-key auth). The signing
  secret returned by `create` is shown once.

## [0.2.0] - 2026-06-02

### Added

- Read-only `tags` field on `Rerout::Models::Link` — an array of
  `Rerout::Models::Tag` value objects (`{ id, name, color }`). Populated on
  `get`, `list`, and `update`; an empty array on `create`. Tag writes are not
  supported for API-key clients.

### Notes

- The project stats endpoint `/v1/projects/me/stats` (used by
  `project.stats`) is now live.

## [0.1.0] - 2026-05-20

### Added

- Initial public release.
- `Rerout::Client` with `links`, `project`, and `qr` namespaces and an
  injectable Faraday connection for tests and edge deployments.
- Link operations: `create`, `list`, `get`, `update`, `delete`, `stats`.
- Project operations: `stats`, `me`.
- QR helpers: pure URL builder (`qr.url`) and authenticated SVG fetch
  (`qr.svg`).
- `Rerout::CreateLinkInput` and `Rerout::UpdateLinkInput` request bodies, with
  the `Rerout::CLEAR` sentinel to distinguish "leave alone" from "set null".
- `Rerout::QrOptions` with `ecc` validation and `refresh` coercion.
- Frozen value models: `Link`, `LinkStats`, `ProjectStats`, `DailyClicksPoint`,
  `StatsBreakdown`, `ListLinksResult`, `Project`.
- `Rerout::Webhooks.verify_signature` (and the `Rerout.verify_signature`
  shortcut) — HMAC-SHA256 webhook signature verification with configurable
  timestamp tolerance and constant-time comparison.
- `Rerout::Error` with stable `code`, `status`, `path`, `timestamp`, `details`
  plus `rate_limited?` and `server_error?` convenience flags.

[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/ruby-v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/ruby-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/ruby-v0.1.0
