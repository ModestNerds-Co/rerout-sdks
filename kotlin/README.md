# Rerout Kotlin SDK

Official Kotlin/JVM client for the [Rerout](https://rerout.co) API — branded
link infrastructure on the edge.

Create, list, update, and delete short links; read project and per-link
analytics; build branded QR URLs; and verify inbound webhook signatures. Every
network call is a `suspend` function and throws a typed `ReroutException` on
failure.

- Kotlin 2.x, JVM 17+
- [OkHttp](https://square.github.io/okhttp/) transport
- `kotlinx.serialization` models

## Install

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("co.rerout:rerout-kotlin:0.2.0")
}
```

Gradle (Groovy DSL):

```groovy
implementation 'co.rerout:rerout-kotlin:0.2.0'
```

Maven:

```xml
<dependency>
  <groupId>co.rerout</groupId>
  <artifactId>rerout-kotlin</artifactId>
  <version>0.2.0</version>
</dependency>
```

## Hello world

```kotlin
import co.rerout.sdk.CreateLinkInput
import co.rerout.sdk.Rerout
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val rerout = Rerout(apiKey = System.getenv("REROUT_API_KEY"))

    val link = rerout.links.create(
        CreateLinkInput(
            targetUrl = "https://example.com/q4-sale",
            domainHostname = "go.brand.com",
            code = "q4",
        ),
    )

    println(link.shortUrl) // https://go.brand.com/q4
}
```

## Construction

```kotlin
// Default production base URL (https://api.rerout.co).
val rerout = Rerout(apiKey = "rrk_...")

// Override the base URL for staging or a self-hosted setup. Trailing
// slashes are trimmed.
val staging = Rerout(apiKey = "rrk_...", baseUrl = "https://api.staging.rerout.co")

// Inject a pre-configured OkHttpClient (custom timeouts, interceptors,
// proxies, shared connection pools).
val custom = Rerout(
    apiKey = "rrk_...",
    httpClient = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(10))
        .build(),
)
```

A blank API key throws `ReroutException` with code `missing_api_key`.

## Links

```kotlin
// Create
val link = rerout.links.create(
    CreateLinkInput(targetUrl = "https://example.com/sale", code = "sale"),
)

// List (paginated)
val page = rerout.links.list(cursor = null, limit = 20)
for (l in page.links) println(l.code)
if (page.hasMore) {
    val next = rerout.links.list(cursor = page.nextCursor, limit = 20)
}

// Get
val one = rerout.links.get("sale")

// Each link carries read-only `tags` — a list of { id, name, color }.
// Empty when none are set; tags cannot be written through create/update.
one.tags.forEach { println("${it.name} (${it.color})") }

// Update — only the fields you set are sent. Use clear*() to set a field
// to null on the server; an unset field is left untouched.
val updated = rerout.links.update(
    "sale",
    UpdateLinkInput.builder()
        .targetUrl("https://example.com/sale-extended")
        .clearExpiresAt()
        .seoTitle("Extended Sale")
        .build(),
)

// An empty patch is rejected client-side (code `bad_request`) without
// hitting the API.

// Delete (soft delete)
val result = rerout.links.delete("sale")
println(result.deleted)

// Per-link stats (defaults to 30 days)
val stats = rerout.links.stats("sale", days = 7)
println("${stats.totalClicks} clicks, ${stats.qrScans} QR scans")
```

## Project

```kotlin
// Aggregate stats across the whole project
val projectStats = rerout.project.stats(days = 30)
println(projectStats.totalClicks)
projectStats.daily.forEach { println("${it.day}: ${it.clicks}") }

// The project that owns the current API key
val project = rerout.project.me()
println("${project.name} (${project.slug})")
```

## QR

```kotlin
// Pure URL builder — no network call.
val url = rerout.qr.url("sale", QrOptions(size = 12, ecc = "H"))
// → https://api.rerout.co/v1/links/sale/qr?size=12&ecc=H

// refresh: Enabled emits `refresh=1`; a token is forwarded verbatim.
val busted = rerout.qr.url("sale", QrOptions(refresh = QrOptions.Refresh.Enabled))
val tagged = rerout.qr.url("sale", QrOptions(refresh = QrOptions.Refresh.token("v2")))

// Authenticated SVG fetch — hits the API with the bearer token.
val svg: String = rerout.qr.svg("sale", QrOptions(size = 16))
```

The QR endpoint is API-key authenticated, so `url()` output cannot be embedded
directly in an `<img>` tag in a browser — proxy it server-side or use `svg()`.

## Webhook signature verification

Rerout signs every webhook delivery with an `X-Rerout-Signature` header in the
form `t={unix_seconds},v1={hex_hmac_sha256}`. The SDK ships a standalone
verifier that handles timestamp tolerance and constant-time comparison.

```kotlin
import co.rerout.sdk.verifyReroutSignature

val ok = verifyReroutSignature(
    rawBody = rawRequestBody,
    signatureHeader = request.getHeader("X-Rerout-Signature"),
    secret = System.getenv("REROUT_WEBHOOK_SECRET"),
)
if (!ok) {
    // reject the request
}
```

`ReroutWebhooks.verifySignature(...)` is the equivalent object-method form. The
default tolerance window is 300 seconds — pass `toleranceSeconds = 0` to
disable the staleness check, or pass a `now` lambda for deterministic tests.

## Error handling

Every call throws `ReroutException` on failure:

```kotlin
try {
    rerout.links.get("does-not-exist")
} catch (e: ReroutException) {
    println(e.code)    // e.g. "not_found"
    println(e.status)  // e.g. 404, or 0 for network failures
    if (e.isRateLimited) { /* back off and retry */ }
    if (e.isServerError) { /* 5xx — server-side issue */ }
}
```

`code` carries the API's stable identifier (`bad_target_url`, `rate_limited`,
…) when the server sends a JSON error body. When it does not, a synthetic code
is used: `unauthorized` (401), `forbidden` (403), `not_found` (404),
`rate_limited` (429), `server_error` (5xx), `client_error` (other 4xx),
`network_error`, `timeout`, `unexpected_response`.

## License

MIT — see [LICENSE](./LICENSE). A copy of the workspace license lives at the
[repository root](https://github.com/ModestNerds-Co/rerout-sdks/blob/main/LICENSE).

## Repository

<https://github.com/ModestNerds-Co/rerout-sdks>

Built and maintained by [Codecraft Solutions](https://codecraftsolutions.co.za).
