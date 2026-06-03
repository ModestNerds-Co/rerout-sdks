# Changelog

All notable changes to the `rerout` crate are documented in this file. The
format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and
this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-06-03

### Added

- Webhook endpoint management via `Rerout::webhooks()` — `create`, `list`, and
  `delete` against `/v1/projects/me/webhooks` (API-key auth). The signing secret
  returned by `create` is shown once.

## [0.2.0] - 2026-06-02

### Added

- Read-only `tags` field on `Link` — a `Vec<Tag>` where each `Tag` carries
  `{ id, name, color }`. Returned by `get`, `list`, and `update`; empty on
  `create`. Tag writes are ignored for API-key clients.
- `Tag` is re-exported from the crate root.

### Notes

- The project stats endpoint `/v1/projects/me/stats` (used by
  `project().stats`) is now live.

## [0.1.0] - 2026-05-22

### Added

- Initial public release.
- `Rerout` async client with `links()`, `project()`, and `qr()` namespaces,
  constructed through `Rerout::builder` (configurable `base_url`, `timeout`,
  `user_agent`, and an injectable `reqwest::Client`).
- Link operations: `create`, `list`, `get`, `update`, `delete`, `stats`.
- Project operations: `stats`, `me`.
- QR helpers: pure URL builder (`qr().url`) and authenticated SVG fetch
  (`qr().svg`), plus the standalone `build_qr_url` function.
- `CreateLinkInput` and `UpdateLinkInput` builders; `UpdateLinkInput` uses the
  `Option<Option<T>>` pattern with `set_*` / `clear_*` methods to distinguish
  "leave the field alone" from "clear it on the server", and rejects an empty
  payload client-side.
- `webhooks::verify_rerout_signature` — HMAC-SHA256 webhook signature
  verification with configurable timestamp tolerance and a constant-time
  comparison via `subtle`.
- `ReroutError` enum with stable `code`, `status`, and `message` accessors;
  optional `path`, `timestamp`, and `details` from the API error envelope; and
  `is_rate_limited` / `is_server_error` convenience flags.
- `rustls`-backed TLS with no OpenSSL dependency.
- `#![forbid(unsafe_code)]` across the crate.

[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/rust/v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/rust-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/rust-v0.1.0
