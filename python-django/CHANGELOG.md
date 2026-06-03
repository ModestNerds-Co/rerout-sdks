# Changelog

All notable changes to `rerout-django` are documented in this file. The format
is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-06-03

### Changed

- Bumped to track base `rerout` SDK `0.3.0`, which adds webhook endpoint
  management (create/list/delete). No API changes in this package.

## [0.2.0] - 2026-06-02

### Changed

- Requires `rerout>=0.2`, which adds the read-only `tags` field (a tuple of
  `Tag` objects with `id`, `name`, and `color`) to every `Link`. This
  wrapper re-exports the base SDK's client, so the new field is available
  through `get_rerout_client()` with no API changes here.

## [0.1.0] - 2026-05-22

### Added

- Initial public release.
- Django integration for the [`rerout`](https://pypi.org/project/rerout/)
  base SDK, supporting Django 4.2 and 5.x.
- `rerout_django` app (`AppConfig`) — configuration-only, no models or
  migrations.
- `REROUT` settings dict for `API_KEY`, `WEBHOOK_SECRET`, `BASE_URL`,
  `TIMEOUT`, and `SIGNATURE_TOLERANCE_SECONDS`.
- `get_rerout_client()` — a process-wide, lazily built, cached
  `rerout.Rerout` instance; `reset_rerout_client()` to rebuild it.
- `WebhookView` — a CSRF-exempt view that verifies the `X-Rerout-Signature`
  header with the base SDK's `verify_rerout_signature`, parses the JSON
  body, and returns `200` / `401` / `400`.
- Bundled `rerout_django.urls` URLConf mounting `WebhookView` at
  `webhook/` with the name `rerout_webhook`.
- Django signals — `rerout_webhook_received` (fired for every verified
  delivery) plus `rerout_link_created`, `rerout_link_updated`,
  `rerout_link_deleted`, `rerout_link_clicked`, and `rerout_qr_scanned`
  for type-specific subscription.
- PEP 561 marker (`py.typed`) — full type hints.

[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/python-django-v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/python-django-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/python-django-v0.1.0
