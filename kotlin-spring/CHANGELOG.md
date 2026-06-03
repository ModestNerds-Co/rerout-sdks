# Changelog

All notable changes to the `co.rerout:rerout-spring-boot-starter` package are
documented in this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2026-06-03

### Changed

- Bumped the base Rerout SDK dependency to `0.3.0`, which adds webhook endpoint
  management (create/list/delete). No API changes in this package.

## [0.2.0] - 2026-06-02

### Changed

- Bumped the base `co.rerout:rerout-kotlin` dependency to `0.2.0`, which adds
  the read-only `tags` field (`{ id, name, color }`) to `Link`. The starter
  re-exports the base SDK models unchanged, so consumers pick up `Link.tags`
  transitively with no API change here.

## [0.1.0] - 2026-05-22

### Added

- Initial public release.
- `ReroutAutoConfiguration` — Spring Boot auto-configuration registered via
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Auto-configured `co.rerout.sdk.Rerout` client bean, built from
  `rerout.api-key` / `rerout.base-url`. Created only when an API key is set and
  backs off if the application supplies its own `Rerout` bean.
- `ReroutProperties` — `@ConfigurationProperties("rerout")` binding for the
  client and webhook settings, including a nested `rerout.webhook.*` group.
- `ReroutWebhookController` — auto-registered REST controller (servlet web apps
  only) that verifies the `X-Rerout-Signature` HMAC, parses the payload, and
  dispatches it to every registered `ReroutWebhookHandler`. Mapped to a
  configurable path (`rerout.webhook.path`).
- `ReroutEvent` — sealed event hierarchy (`LinkCreated`, `LinkUpdated`,
  `LinkDeleted`, `LinkClicked`, `Unknown`) with a `ReroutEvent.parse` helper.
- `ReroutWebhookHandler` — functional interface for consuming verified events.
- `maven-publish` configuration targeting Maven Central as
  `co.rerout:rerout-spring-boot-starter`, with sources and javadoc jars.

[0.3.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/kotlin-spring-v0.3.0
[0.2.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/kotlin-spring-v0.2.0
[0.1.0]: https://github.com/ModestNerds-Co/rerout-sdks/releases/tag/kotlin-spring-v0.1.0
