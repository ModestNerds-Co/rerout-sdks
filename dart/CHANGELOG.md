# Changelog

All notable changes to the `rerout` Dart package are documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 0.2.0 - 2026-06-02

### Added

- Read-only `tags` field on `ShortLink` — a list of `Tag` (`{id, name, color}`)
  returned by `get`, `list`, and `update`. Empty on `create`. Parsed leniently
  (defaults to an empty list when absent).
- `Tag` model.

### Notes

- The project stats endpoint `/v1/projects/me/stats` is now live.

## 0.1.0 - 2026-05-20

### Added

- Initial public release.
- `Rerout.initialize` factory with default + custom `baseUrl` and injectable
  `Dio` for tests.
- Link operations: `create`, `list`, `get`, `update`, `delete`, `stats`.
- Project operations: `stats`, `me`.
- QR helpers: pure URL builder (`Rerout.qr.url`) and authenticated SVG fetch
  (`Rerout.qr.svg`).
- `ReroutWebhookSignature.verify` — HMAC-SHA256 webhook signature verification
  with configurable timestamp tolerance and constant-time comparison.
- Sealed `Result<T>` type (`Success`/`Error`) with `map`, `flatMap`, `when`.
- `ReroutException` with `code`, `statusCode`, `message`, `path`, `timestamp`
  plus `isServerError` / `isRateLimited` flags.
