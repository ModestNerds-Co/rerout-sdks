# Changelog

All notable changes to the `Rerout` .NET package are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.0] - 2026-06-04

### Added

- Smart Links support on `Link`: read-only `PasswordProtected`, `MaxClicks`,
  `ClickCount`, `TrackConversions`, `RoutingRules` (`IReadOnlyList<RoutingRule>`),
  and `AbVariants` (`IReadOnlyList<AbVariant>`). New `RoutingRule` and `AbVariant`
  records; all default sensibly when the server omits them.
- Smart Links input on `CreateLinkInput`: optional `Password`, `MaxClicks`,
  `TrackConversions`, `RoutingRules`, and `AbVariants` (items via the new
  `AbVariantInput`, whose `Weight` is optional).
- Smart Links input on `UpdateLinkInput`: `Password` and `MaxClicks` as
  `Optional<…?>` (set, or `Set(null)` to clear), `TrackConversions`, and
  full-replace `RoutingRules` / `AbVariants`.
- `Conversions` accessor — `RecordAsync(RecordConversionInput)` POSTs to
  `/v1/conversions` and returns `RecordedConversion` (`Recorded`, `Duplicate`).
- `Links.CreateBatchAsync(IReadOnlyList<BatchLinkInput>)` — POSTs to
  `/v1/links/batch` and returns a partial-success `BatchCreateLinksResult`
  (`Created`, `Total`, per-item `BatchLinkResult`).
- New webhook event type `conversion.recorded` is now deliverable.

## [0.3.0] - 2026-06-03

### Added

- Webhook endpoint management via the `Webhooks` accessor — `CreateAsync`,
  `ListAsync`, and `DeleteAsync` against `/v1/projects/me/webhooks` (API-key
  auth). The signing secret returned by `CreateAsync` is shown once.

## [0.2.0] - 2026-06-02

### Added

- Read-only `Tags` property on `Link` — a list of `Tag` records (`Id`, `Name`,
  `Color`). Populated by the API on get/list/update responses and empty on
  create; defaults to an empty list when absent.

### Changed

- The project stats endpoint `/v1/projects/me/stats` (used by
  `Project.StatsAsync`) is now live.

## [0.1.0] - 2026-05-22

### Added

- Initial public release.
- `ReroutClient` with `Links`, `Project`, and `Qr` namespaces; accepts a
  required API key plus optional `ReroutClientOptions` (custom `BaseUrl`,
  per-request `Timeout`, injectable `HttpClient`).
- Link operations: `CreateAsync`, `ListAsync`, `GetAsync`, `UpdateAsync`,
  `DeleteAsync`, `StatsAsync`.
- Project operations: `StatsAsync`, `MeAsync`.
- QR helpers: pure `Url` builder and authenticated `SvgAsync` fetch.
- `Optional<T>` three-state value for `UpdateLinkInput` — omit, set, or
  explicitly clear a field; empty updates fail client-side with `empty_update`.
- `SignatureVerifier.Verify` — HMAC-SHA256 webhook signature verification with
  configurable timestamp tolerance and constant-time comparison via
  `CryptographicOperations.FixedTimeEquals`.
- `ReroutException` with stable `Code`, `Status`, `Message`, `Path`,
  `Timestamp`, and `Details`, plus `IsServerError` / `IsRateLimited` flags.
- `snake_case` JSON (de)serialization on `System.Text.Json`; no third-party
  dependencies.
- Targets `net8.0`.

[0.4.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/dotnet/v0.4.0
[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/dotnet/v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/dotnet-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/dotnet-v0.1.0
