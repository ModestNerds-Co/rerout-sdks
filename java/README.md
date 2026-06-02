# Rerout Java SDK

Official Java client for the [Rerout](https://rerout.co) API — branded link
infrastructure on the edge.

Create, list, update, and delete short links; read project and per-link
analytics; build branded QR URLs; and verify inbound webhook signatures.

- Pure Java 17 — no Kotlin runtime, no Scala, no framework
- HTTP via the JDK's built-in `java.net.http.HttpClient`
- JSON via [Gson](https://github.com/google/gson) — the only runtime dependency
- Every network call ships in a **blocking** and an **async** form

## Install

Gradle (Groovy DSL):

```groovy
dependencies {
    implementation 'co.rerout:rerout-java:0.1.0'
}
```

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("co.rerout:rerout-java:0.1.0")
}
```

Maven:

```xml
<dependency>
  <groupId>co.rerout</groupId>
  <artifactId>rerout-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Hello world

```java
import co.rerout.sdk.Rerout;
import co.rerout.sdk.model.CreateLinkInput;
import co.rerout.sdk.model.Link;

public class Example {
    public static void main(String[] args) {
        Rerout rerout = Rerout.create(System.getenv("REROUT_API_KEY"));

        Link link = rerout.links().create(
            CreateLinkInput.builder("https://example.com/q4-sale")
                .domainHostname("go.brand.com")
                .code("q4")
                .build());

        System.out.println(link.getShortUrl()); // https://go.brand.com/q4
    }
}
```

## Blocking vs async

Every `links`, `project`, and `qr.svg` operation exists in two forms:

- **Blocking** — `Link create(CreateLinkInput)` returns the value directly and
  throws `ReroutException` on failure.
- **Async** — `CompletableFuture<Link> createAsync(CreateLinkInput)` is
  non-blocking and completes exceptionally with `ReroutException`.

The async form is the primary path — it runs directly on
`HttpClient.sendAsync`. The blocking form simply joins it and unwraps the
`CompletionException` so callers always see a clean `ReroutException`. Pick
whichever suits your call site; both reach the same transport.

```java
// Blocking
Link link = rerout.links().get("q4");

// Async
rerout.links().getAsync("q4")
    .thenAccept(l -> System.out.println(l.getShortUrl()))
    .exceptionally(err -> { err.printStackTrace(); return null; });
```

`qr.url(...)` is a pure synchronous builder — it has no async form because it
makes no network call.

## Construction

```java
// Default production base URL (https://api.rerout.co), 30s timeout.
Rerout rerout = Rerout.create("rrk_...");

// Builder for advanced configuration.
Rerout custom = Rerout.builder("rrk_...")
    .baseUrl("https://api.staging.rerout.co")   // trailing slashes trimmed
    .timeout(Duration.ofSeconds(10))
    .httpClient(HttpClient.newHttpClient())     // custom executor, proxy, SSL…
    .build();
```

A blank or `null` API key throws `ReroutException` with code `missing_api_key`.
A `Rerout` instance is immutable and thread-safe — reuse one across your app.

## Links

```java
// Create
Link link = rerout.links().create(
    CreateLinkInput.builder("https://example.com/sale").code("sale").build());

// List (paginated)
ListLinksResult page = rerout.links().list(null, 20);
for (Link l : page.getLinks()) {
    System.out.println(l.getCode());
}
if (page.hasMore()) {
    ListLinksResult next = rerout.links().list(page.getNextCursor(), 20);
}

// Get
Link one = rerout.links().get("sale");

// Read-only tags — populated by get/list/update, empty for a freshly
// created link. getTags() never returns null.
for (Tag tag : one.getTags()) {
    System.out.println(tag.getName() + " (" + tag.getColor() + ")");
}

// Update — only the fields you set are sent. Use clear*() to set a field
// to null on the server; an unset field is left untouched.
Link updated = rerout.links().update("sale",
    UpdateLinkInput.builder()
        .targetUrl("https://example.com/sale-extended")
        .clearExpiresAt()              // sends "expires_at": null
        .seoTitle("Extended Sale")
        .build());

// An empty patch is rejected client-side (code `bad_request`) without
// hitting the API.

// Delete (soft delete)
DeleteResult result = rerout.links().delete("sale");
System.out.println(result.isDeleted());

// Per-link stats (defaults to 30 days)
LinkStats stats = rerout.links().stats("sale", 7);
System.out.println(stats.getTotalClicks() + " clicks, "
    + stats.getQrScans() + " QR scans");
```

### Leave-alone vs clear-to-null

`UpdateLinkInput` distinguishes *"leave the field alone"* from *"set the field
to null on the server"*:

- A setter such as `seoTitle("…")` sends the new value.
- A `clear*()` method such as `clearSeoTitle()` sends an explicit JSON `null`.
- A field touched by neither is omitted entirely — server state is untouched.

If both `expiresAt(…)` and `clearExpiresAt()` are called on the same builder,
the clear flag wins.

## Project

```java
// Aggregate stats across the whole project
ProjectStats projectStats = rerout.project().stats(30);
System.out.println(projectStats.getTotalClicks());
for (DailyClicksPoint point : projectStats.getDaily()) {
    System.out.println(point.getDay() + ": " + point.getClicks());
}

// The project that owns the current API key
ProjectInfo project = rerout.project().me();
System.out.println(project.getName() + " (" + project.getSlug() + ")");
```

## QR

```java
// Pure URL builder — no network call.
String url = rerout.qr().url("sale",
    QrOptions.builder().size(12).ecc("H").build());
// → https://api.rerout.co/v1/links/sale/qr?size=12&ecc=H

// refreshEnabled() emits `refresh=1`; refreshToken(…) is forwarded verbatim.
String busted = rerout.qr().url("sale",
    QrOptions.builder().refreshEnabled().build());
String tagged = rerout.qr().url("sale",
    QrOptions.builder().refreshToken("v2").build());

// Authenticated SVG fetch — hits the API with the bearer token.
String svg = rerout.qr().svg("sale", QrOptions.builder().size(16).build());
```

The QR endpoint is API-key authenticated, so `url()` output cannot be embedded
directly in a browser `<img>` tag — proxy it server-side or use `svg()`.

## Webhook signature verification

Rerout signs every webhook delivery with an `X-Rerout-Signature` header in the
form `t={unix_seconds},v1={hex_hmac_sha256}`. The SDK ships a standalone
verifier that handles timestamp tolerance and constant-time comparison.

```java
import co.rerout.sdk.Webhooks;

boolean ok = Webhooks.verifyReroutSignature(
    rawRequestBody,
    request.getHeader("X-Rerout-Signature"),
    System.getenv("REROUT_WEBHOOK_SECRET"));
if (!ok) {
    // reject the request
}
```

The default tolerance window is 300 seconds. An overload takes a custom
tolerance (`0` disables the staleness check) and an injectable `LongSupplier`
clock for deterministic tests:

```java
boolean ok = Webhooks.verifyReroutSignature(
    rawBody, header, secret, /* toleranceSeconds */ 0, /* clock */ () -> 1_716_000_000L);
```

The HMAC is compared with `MessageDigest.isEqual` for constant-time safety.

## Error handling

Every blocking call throws `ReroutException` on failure; every async call
completes the future exceptionally with it.

```java
try {
    rerout.links().get("does-not-exist");
} catch (ReroutException e) {
    System.out.println(e.getCode());    // e.g. "not_found"
    System.out.println(e.getStatus());  // e.g. 404, or 0 for network failures
    if (e.isRateLimited()) { /* back off and retry */ }
    if (e.isServerError()) { /* 5xx — server-side issue */ }
}
```

`getCode()` carries the API's stable identifier (`bad_target_url`,
`rate_limited`, …) when the server sends a JSON error body. When it does not, a
synthetic code is used: `unauthorized` (401), `forbidden` (403), `not_found`
(404), `rate_limited` (429), `server_error` (5xx), `client_error` (other 4xx),
`network_error`, `timeout`, `unexpected_response`, `missing_api_key`,
`bad_request`.

## Building from source

```bash
./gradlew build      # compile + run the full test suite
```

The repository ships the Gradle 8.13 wrapper. The build pins its JVM to a JDK
17 toolchain via `gradle.properties` (Gradle 8.x cannot run on a JDK 25
runtime) — adjust `org.gradle.java.home` if your JDK 17 lives elsewhere.

## License

MIT — see [LICENSE](./LICENSE). A copy of the workspace license lives at the
[repository root](https://github.com/ModestNerds-Co/rerout-sdks/blob/main/LICENSE).

## Repository

<https://github.com/ModestNerds-Co/rerout-sdks>

Built and maintained by [Codecraft Solutions](https://codecraftsolutions.co.za).
