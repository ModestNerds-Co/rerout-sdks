# Changelog

All notable changes to the `co.rerout:rerout-java` package are documented in
this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Tag management via `rerout.tags()` — `list`, `create`, `update`, and `delete`
  against `/v1/projects/me/tags` (API-key auth), each in a blocking and an
  `…Async` form. `list` returns each tag with its live `linkCount`; `create`
  and `update` return a plain `Tag`. `update` sends only the fields you set and
  rejects an empty patch client-side (`bad_request`), matching `links.update`.
- New model types: `TagSummary` (`Tag` + `linkCount`), `ListTagsResult`,
  `CreateTagInput`, and `UpdateTagInput`, the inputs with fluent builders.

## [0.4.0] - 2026-06-04

### Added

- Smart Links support on `Link`, `CreateLinkInput`, and `UpdateLinkInput`:
  - `Link` gains `isPasswordProtected()`, `getMaxClicks()` (`Long`, nullable),
    `getClickCount()`, `isTrackConversions()`, `getRoutingRules()`
    (`List<RoutingRule>`), and `getAbVariants()` (`List<AbVariant>`). Missing
    fields parse to safe defaults; the list getters never return `null`.
  - `CreateLinkInput.Builder` gains `password`, `maxClicks`,
    `trackConversions`, `routingRules`, and `abVariants`.
  - `UpdateLinkInput.Builder` gains `password` / `clearPassword`, `maxClicks` /
    `clearMaxClicks`, `trackConversions`, `routingRules`, and `abVariants`.
    `clearPassword` / `clearMaxClicks` send explicit JSON `null`; `routingRules`
    and `abVariants` are a full replace (an empty list replaces with nothing).
  - New model types: `RoutingRule`, `AbVariant`, `AbVariantInput`.
- Conversion tracking via `rerout.conversions()` — `record` / `recordAsync`
  against `POST /v1/conversions`, returning `RecordedConversion`
  (`isRecorded()`, `isDuplicate()`). New `RecordConversionInput` request model.
- Batch link creation via `rerout.links().createBatch(...)` /
  `createBatchAsync(...)` against `POST /v1/links/batch`, returning
  `BatchCreateLinksResult` (`getCreated()`, `getTotal()`, `getResults()`). New
  model types: `BatchLinkInput`, `BatchLinkResult`, `BatchCreateLinksResult`.

### Notes

- New webhook event type `conversion.recorded` is now deliverable.

## [0.3.0] - 2026-06-03

### Added

- Webhook endpoint management via `rerout.webhooks()` — `create`, `list`, and
  `delete` against `/v1/projects/me/webhooks` (API-key auth). The signing secret
  returned by `create` is shown once.

## [0.2.0] - 2026-06-02

### Added

- Read-only `tags` field on `Link` — a `List<Tag>`, where each `Tag` carries
  `{ id, name, color }`. Populated by `get`, `list`, and `update`; an empty
  list on `create`. A missing or `null` `tags` field in a response is parsed
  leniently into an empty list, and `Link.getTags()` never returns `null`.
  Tag writes are not accepted from API-key clients, so no tag-write input field
  was added.

### Changed

- The project stats endpoint `/v1/projects/me/stats` (targeted by
  `Rerout.project().stats(...)`) is now live server-side.

## [0.1.0] - 2026-05-22

### Added

- Initial public release.
- `Rerout` client with `links()`, `project()`, and `qr()` namespaces.
- Construction via `Rerout.create(apiKey)` and a `Rerout.builder(apiKey)` that
  accepts a custom `baseUrl`, per-request `timeout`, JDK `HttpClient`, and
  `Gson` instance.
- Dual blocking + async API: every `links` / `project` / `qr.svg` operation
  ships as a blocking method (returns the value, throws `ReroutException`) and
  an `…Async` method returning a `CompletableFuture` that completes
  exceptionally with `ReroutException`. The async form is the primary path
  (`HttpClient.sendAsync`); the blocking form joins it.
- Link operations: `create`, `list`, `get`, `update`, `delete`, `stats`.
- Project operations: `stats`, `me`.
- QR helpers: a pure synchronous URL builder (`Rerout.qr().url`) and an
  authenticated SVG fetch (`Rerout.qr().svg` / `svgAsync`).
- `CreateLinkInput` and `UpdateLinkInput`, both with fluent builders.
  `UpdateLinkInput` distinguishes "leave unchanged" from "clear to null" via
  explicit `clear*()` builder methods.
- `Webhooks.verifyReroutSignature` — HMAC-SHA256 webhook signature
  verification with configurable timestamp tolerance, an injectable clock for
  tests, and constant-time comparison via `MessageDigest.isEqual`.
- `ReroutException` (unchecked) with a stable `code`, `status`, `path`,
  `timestamp`, and `details`; `isRateLimited()` / `isServerError()`
  convenience flags.
- Immutable model types for every API shape: `Link`, `ListLinksResult`,
  `DeleteResult`, `ProjectInfo`, `ProjectStats`, `LinkStats`, `StatsBreakdown`,
  `DailyClicksPoint`, `QrOptions`.
- Pure Java 17 — HTTP via the JDK's `java.net.http.HttpClient`, JSON via Gson,
  with zero Kotlin or framework runtime dependency.
- `maven-publish` configuration targeting Maven Central as
  `co.rerout:rerout-java`, with sources and javadoc jars.

[0.4.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/java/v0.4.0
[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/java/v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/java-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/java-v0.1.0
