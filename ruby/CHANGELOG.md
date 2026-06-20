# Changelog

All notable changes to the `rerout` gem are documented in this file. The
format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- New `tags` namespace for tag management — `client.tags.list`, `create`,
  `update(tag_id, input)`, and `delete(tag_id)` against `/v1/projects/me/tags`
  (API-key auth; project resolved from the key).
- `Rerout::CreateTagInput` (required `name`, optional `color`) and
  `Rerout::UpdateTagInput` (both fields optional; omitted fields are left
  unchanged, no client-side empty-payload check) request bodies. Both also
  accept a plain Hash.
- New value models `Rerout::Models::TagSummary` (`Tag` plus `link_count`, the
  list-response shape) and `Rerout::Models::ListTagsResult`. `tags.create` /
  `tags.update` return the existing `Rerout::Models::Tag` (`{ id, name, color }`).

## [0.4.0] - 2026-06-04

### Added

- Smart Link fields on `Rerout::Models::Link`: `password_protected`,
  `max_clicks`, `click_count`, `track_conversions`, `routing_rules` (array of
  `RoutingRule`), and `ab_variants` (array of `AbVariant`).
- `Rerout::CreateLinkInput` gains optional `password`, `max_clicks`,
  `track_conversions`, `routing_rules`, and `ab_variants`.
- `Rerout::UpdateLinkInput` gains `password`, `max_clicks`,
  `track_conversions`, `routing_rules`, and `ab_variants`. `password` and
  `max_clicks` accept `Rerout::CLEAR` to send `null`; `routing_rules` and
  `ab_variants` are a full replacement.
- New `Rerout::Models::RoutingRule` and `Rerout::Models::AbVariant` value
  objects. Routing-rule `condition_type` is `"country"` / `"device"`;
  `condition_op` is `"is"` / `"is_not"` / `"in"`.
- New `conversions` namespace — `client.conversions.record(click_id,
  event_name, value_cents:, currency:)` against `POST /v1/conversions`,
  returning a `Rerout::Models::RecordedConversion` (`recorded`, `duplicate`).
- New `client.links.create_batch(inputs)` against `POST /v1/links/batch`,
  returning a `Rerout::Models::BatchCreateLinksResult` (`created`, `total`,
  `results` of `BatchLinkResult`).
- New webhook event type `conversion.recorded`.

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

[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/ruby/v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/ruby-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/ruby-v0.1.0
