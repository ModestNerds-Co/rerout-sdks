# Changelog

All notable changes to `rerout` are documented in this file. The format is
based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-05-21

### Added

- Initial public release.
- `Rerout` client with `links`, `project`, and `qr` namespaces.
- Link operations: `create`, `list`, `get`, `update`, `delete`, `stats`.
- Project operations: `stats`, `me`.
- QR helpers: pure URL builder + signed SVG fetch.
- `verify_rerout_signature` — HMAC-SHA256 webhook signature verification
  with configurable timestamp tolerance and constant-time comparison.
- `ReroutError` with stable `code`, `status`, `message`, `details`, `path`,
  `timestamp`; `is_rate_limited`, `is_server_error` convenience flags.
- Frozen, slotted dataclasses for every response shape (`Link`,
  `ListLinksResult`, `LinkStats`, `ProjectStats`, `ProjectInfo`,
  `StatsBreakdown`, `DailyClicksPoint`).
- `UpdateLinkInput` with an `UNSET` sentinel so callers can distinguish
  "leave this field alone" from "clear this field on the server".
- PEP 561 marker (`py.typed`) — full type hints, mypy strict-clean.
- Context-manager support on the client for deterministic teardown.

[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/python-v0.1.0
