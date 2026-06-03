# Changelog

All notable changes to the `rerout-rails` gem are documented in this file. The
format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-06-03

### Changed

- Bumped the base `rerout` gem dependency to allow `0.3.0`, which adds webhook
  endpoint management (create/list/delete). No API changes in this package.

## [0.2.0] - 2026-06-02

### Changed

- Require `rerout ~> 0.2`, which adds the read-only `tags` field
  (`{ id, name, color }`) to `Rerout::Models::Link`. The Rails integration
  re-exports the base SDK models unchanged.

## [0.1.0] - 2026-05-20

### Added

- Initial public release.
- `Rerout::Rails.configure` — initializer-driven configuration with
  environment-variable fallbacks for `api_key`, `webhook_secret`, and
  `base_url`.
- `Rerout::Rails.client` — a process-wide, lazily-built, cached
  `Rerout::Client`.
- `Rerout::Rails::WebhookController` — verifies the `X-Rerout-Signature`
  header and responds `200` / `401` / `400`. CSRF protection is skipped for
  server-to-server deliveries.
- `Rerout::Rails::Events` — dispatches verified deliveries through
  `ActiveSupport::Notifications`: a `rerout.webhook` catch-all plus per-event
  topics (`rerout.link.created`, `rerout.link.updated`, `rerout.link.deleted`,
  `rerout.link.clicked`, `rerout.qr.scanned`).
- `Rerout::Rails::Railtie` — hooks the integration into the Rails boot
  process.
- `rails generate rerout:install` — drops `config/initializers/rerout.rb` and
  mounts the webhook route.
- `Rerout::Rails::ConfigurationError` raised on missing `api_key` or
  `webhook_secret`.

[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/ruby-rails/v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/ruby-rails-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/ruby-rails-v0.1.0
