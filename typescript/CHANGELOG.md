# Changelog

All notable changes to `@rerout/sdk` are documented in this file. The format is
based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-06-02

### Added

- Read-only `tags` field on `Link` (`{ id, name, color }`), returned by
  `links.get`, `links.list`, and `links.update`.

### Changed

- `project.stats()` is now backed by a live `/v1/projects/me/stats` endpoint.

## [0.1.0] - 2026-05-20

### Added

- Initial public release.
- `Rerout` client with `links`, `project`, and `qr` namespaces.
- Link operations: `create`, `list`, `get`, `update`, `delete`, `stats`.
- Project operations: `stats`, `me`.
- QR helpers: pure URL builder + signed SVG fetch.
- `verifyReroutSignature` — HMAC-SHA256 webhook signature verification with
  configurable timestamp tolerance and constant-time comparison.
- `ReroutError` with stable `code`, `status`, `details`; `isRateLimited`,
  `isServerError` convenience flags.
- ESM + CJS dual build with bundled `.d.ts` declarations.

[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/typescript-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/typescript-v0.1.0
