# Changelog

All notable changes to the `rerout` Go SDK are documented in this file. The
format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.0] - 2026-06-04

### Added

- Smart Links support on `Link`: `PasswordProtected`, `MaxClicks`,
  `ClickCount`, `TrackConversions`, `RoutingRules` (`[]RoutingRule` with
  `{ConditionType, ConditionOp, ConditionValue, TargetURL}`), and `ABVariants`
  (`[]ABVariant` with `{ID, TargetURL, Weight}`).
- Smart Links inputs on `CreateLinkInput` (`Password`, `MaxClicks`,
  `TrackConversions`, `RoutingRules`, `ABVariants`) and `UpdateLinkInput`
  (`Password`/`ClearPassword`, `MaxClicks`/`ClearMaxClicks`, `TrackConversions`,
  and full-replace `RoutingRules` / `ABVariants` pointer-to-slice fields).
- New `Conversions` namespace via `Client.Conversions()` — `Record` against
  `POST /v1/conversions`, returning `{Recorded, Duplicate}`.
- Batch link creation via `Links().CreateBatch` — `POST /v1/links/batch`,
  returning `{Created, Total, Results}` with per-item index/ok/code/error.
- New webhook event type `conversion.recorded` is now deliverable.

## [0.3.0] - 2026-06-03

### Added

- Webhook endpoint management via `Client.Webhooks()` — `Create`, `List`, and
  `Delete` against `/v1/projects/me/webhooks` (API-key auth). The signing secret
  returned by `Create` is shown once.

## [0.2.0] - 2026-06-02

### Added

- Read-only `Tags` field on `Link` (`{id, name, color}`), returned by
  `Links().Get`, `Links().List`, and `Links().Update`. Tags cannot be written
  via the API-key client.

### Changed

- The project stats endpoint `/v1/projects/me/stats` (used by
  `Project().Stats`) is now live.

## [0.1.0] - 2026-05-22

### Added

- Initial public release.
- `NewClient` constructor with functional options: `WithBaseURL`,
  `WithHTTPClient`, `WithTimeout`, `WithDefaultHeaders`.
- Three namespaces reached via `Client.Links()`, `Client.Project()`, and
  `Client.QR()`.
- Link operations: `Create`, `List`, `Get`, `Update`, `Delete`, `Stats`.
- Project operations: `Stats`, `Me`.
- QR helpers: pure URL builder (`QR().URL`, plus standalone `BuildQRURL`) and
  authenticated SVG fetch (`QR().SVG`).
- `UpdateLinkInput` with a `ClearXxx`-flag pattern that distinguishes
  "leave untouched", "set a value", and "clear server-side" (explicit JSON
  `null`); empty payloads are rejected client-side.
- `VerifySignature` — standalone HMAC-SHA256 webhook signature verification
  with configurable timestamp tolerance (`WithTolerance`), an injectable clock
  (`WithClock`), and constant-time comparison.
- `ReroutError` with stable `Code`, `Status`, `Message`, `Path`, `Timestamp`,
  and `Details`; `IsRateLimited` / `IsServerError` convenience methods;
  `Unwrap` support and the `AsReroutError` helper.
- Pointer helpers `String`, `Int`, `Int64`, `Bool` for optional struct fields.
- Standard-library only — no third-party dependencies.

[0.4.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/go/v0.4.0
[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/go/v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/go-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/go-v0.1.0
