# Changelog

All notable changes to `@rerout/sdk` are documented in this file. The format is
based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - 2026-06-20

### Added

- `tags` namespace for tag management — `tags.list()` (returns each tag with its
  `link_count`), `tags.create({ name, color? })`, `tags.update(tagId, { name?, color? })`,
  and `tags.delete(tagId)` against `/v1/projects/me/tags` (API-key auth).
- New exported types: `TagSummary`, `ListTagsResult`, `CreateTagInput`,
  `UpdateTagInput`, and the `Tags` namespace class.

## [0.4.0] - 2026-06-04

### Added

- Smart Links support on `Link`: `password_protected`, `max_clicks`,
  `click_count`, `track_conversions`, `routing_rules` (`RoutingRule`), and
  `ab_variants` (`AbVariant`).
- `CreateLinkInput` gains `password`, `max_clicks`, `track_conversions`,
  `routing_rules`, and `ab_variants`. `UpdateLinkInput` gains the same fields,
  with `password` / `max_clicks` accepting `null` to clear and
  `routing_rules` / `ab_variants` performing a full replace.
- `conversions` namespace — `conversions.record(...)` posts to
  `/v1/conversions` and returns `{ recorded, duplicate }`.
- `links.createBatch(...)` posts to `/v1/links/batch` and returns
  `{ created, total, results }` for partial-success bulk creation.
- New webhook event type `conversion.recorded` is delivered by the server.

## [0.3.0] - 2026-06-03

### Added

- Webhook endpoint management via a new `webhooks` namespace — `create`, `list`,
  and `delete` against `/v1/projects/me/webhooks` (API-key auth). The
  `signing_secret` returned by `create` is shown once.

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

[0.5.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/typescript/v0.5.0
[0.4.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/typescript/v0.4.0
[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/typescript/v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/typescript-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/typescript-v0.1.0
