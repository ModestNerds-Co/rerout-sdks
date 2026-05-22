# Rerout Spring Boot Starter

Spring Boot auto-configuration for the [Rerout](https://rerout.co) API —
branded link infrastructure on the edge.

This starter wires the [`co.rerout:rerout-kotlin`](../kotlin) SDK into a Spring
Boot application: it auto-configures a ready-to-inject `Rerout` client from
your `application.yml`, and registers a webhook controller that verifies and
dispatches inbound Rerout events.

- Spring Boot 3.3+, JVM 17+
- Auto-configured `Rerout` client bean
- Signature-verified inbound webhook controller
- Typed `ReroutEvent` sealed hierarchy

## Install

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("co.rerout:rerout-spring-boot-starter:0.1.0")
}
```

Maven:

```xml
<dependency>
  <groupId>co.rerout</groupId>
  <artifactId>rerout-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

The starter pulls in `co.rerout:rerout-kotlin` transitively.

## Configuration

Bind everything under the `rerout` namespace in `application.yml`:

```yaml
rerout:
  api-key: ${REROUT_API_KEY}
  base-url: https://api.rerout.co        # optional, defaults to production
  webhook:
    secret: ${REROUT_WEBHOOK_SECRET}     # enables the webhook controller
    enabled: true                        # optional, default true
    path: /webhooks/rerout               # optional, default /webhooks/rerout
    tolerance-seconds: 300               # optional, default 300; 0 disables
```

| Property | Default | Purpose |
|---|---|---|
| `rerout.api-key` | — | Project API key. The `Rerout` bean is created only when set. |
| `rerout.base-url` | `https://api.rerout.co` | API base URL. |
| `rerout.webhook.secret` | — | Endpoint signing secret. The webhook controller registers only when set. |
| `rerout.webhook.enabled` | `true` | Toggle the webhook controller. |
| `rerout.webhook.path` | `/webhooks/rerout` | Path the controller is mapped to. |
| `rerout.webhook.tolerance-seconds` | `300` | Signature timestamp tolerance. |

## Using the client

When `rerout.api-key` is set, a `Rerout` bean is available for injection:

```kotlin
import co.rerout.sdk.CreateLinkInput
import co.rerout.sdk.Rerout
import org.springframework.stereotype.Service

@Service
class LinkService(private val rerout: Rerout) {

    suspend fun shorten(target: String): String {
        val link = rerout.links.create(CreateLinkInput(targetUrl = target))
        return link.shortUrl
    }
}
```

The bean backs off if your application defines its own `Rerout` bean, so you
can fully customise construction when needed.

See the [`rerout-kotlin` README](../kotlin/README.md) for the full client
surface — `links`, `project`, `qr`, and the `ReroutException` error model.

## Receiving webhooks

When `rerout.webhook.secret` is set, the starter registers a controller mapped
to `rerout.webhook.path`. It verifies the `X-Rerout-Signature` HMAC, parses the
body into a typed `ReroutEvent`, and dispatches it to every `ReroutWebhookHandler`
bean.

Implement a handler:

```kotlin
import co.rerout.spring.ReroutEvent
import co.rerout.spring.ReroutWebhookHandler
import org.springframework.stereotype.Component

@Component
class ClickLogger : ReroutWebhookHandler {
    override fun onEvent(event: ReroutEvent) {
        when (event) {
            is ReroutEvent.LinkClicked ->
                println("click on ${event.code} from ${event.country}")
            is ReroutEvent.LinkCreated ->
                println("link created: ${event.code}")
            is ReroutEvent.Unknown ->
                println("unhandled event type: ${event.type}")
            else -> {}
        }
    }
}
```

Controller responses:

- `200` — signature verified, event parsed and dispatched.
- `400` — missing/invalid signature, stale timestamp, or an unparseable body.
- `500` — a handler threw; Rerout will retry the delivery.

### Webhook events

`ReroutEvent` is a sealed hierarchy:

| Event | `type` | Notes |
|---|---|---|
| `ReroutEvent.LinkCreated` | `link.created` | `code`, `shortUrl` |
| `ReroutEvent.LinkUpdated` | `link.updated` | `code` |
| `ReroutEvent.LinkDeleted` | `link.deleted` | `code` |
| `ReroutEvent.LinkClicked` | `link.clicked` | `code`, `country`, `qr` |
| `ReroutEvent.Unknown` | _any other_ | full `payload` preserved |

Each event also carries the raw `data` (or `payload`) JSON so handlers can read
fields the SDK does not model yet.

## Overriding the defaults

- Define your own `Rerout` bean to control client construction.
- Define your own `ReroutWebhookController` bean to fully replace the built-in
  controller.
- Set `rerout.webhook.enabled=false` to disable the webhook controller while
  still using the auto-configured client.

## License

MIT — see [LICENSE](./LICENSE). A copy of the workspace license lives at the
[repository root](https://github.com/ModestNerds-Co/rerout-sdks/blob/main/LICENSE).

## Repository

<https://github.com/ModestNerds-Co/rerout-sdks>

Built and maintained by [Codecraft Solutions](https://codecraftsolutions.co.za).
