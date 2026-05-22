# Changelog

All notable changes to `rerout/laravel` are documented in this file. The
format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-05-20

### Added

- Initial public release.
- `ReroutServiceProvider` — auto-discovered provider that merges the
  `config/rerout.php` config, binds a shared `Rerout\Rerout` client singleton
  (resolvable by class or the `rerout` alias), registers the webhook route,
  and the `rerout:ping` command.
- `Rerout` facade over the bound client.
- `config/rerout.php` — publishable config reading `REROUT_API_KEY`,
  `REROUT_BASE_URL`, `REROUT_TIMEOUT`, `REROUT_WEBHOOK_SECRET`, and webhook
  route/tolerance settings from the environment.
- `WebhookController` — verifies the `X-Rerout-Signature` header, returns
  `401` for invalid signatures, `400` for non-object payloads, dispatches a
  Laravel event and returns `200` otherwise. Mounted on a configurable,
  named `rerout.webhook` route by default.
- Webhook events `LinkClicked`, `QrScanned`, and `DomainFailed`, each carrying
  the decoded payload.
- `rerout:ping` Artisan command — verifies the configured API key against
  `GET /v1/projects/me`.
- Supports Laravel 10, 11, and 12 on PHP 8.2+.

[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/php-laravel-v0.1.0
