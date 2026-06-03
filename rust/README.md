# rerout

Official Rust SDK for the [Rerout](https://rerout.co) API.

Branded link infrastructure on Cloudflare. Create short links, render QR codes,
read analytics, and verify webhook signatures.

## Install

```bash
cargo add rerout
```

Or add it to `Cargo.toml`:

```toml
[dependencies]
rerout = "0.1"
tokio = { version = "1", features = ["macros", "rt-multi-thread"] }
```

Every method is async and requires a [Tokio](https://tokio.rs) runtime. The
crate ships with `rustls` TLS and pulls in no OpenSSL dependency.

## Usage

```rust,no_run
use rerout::{CreateLinkInput, Rerout};

#[tokio::main]
async fn main() -> Result<(), rerout::ReroutError> {
    let rerout = Rerout::new(std::env::var("REROUT_API_KEY").unwrap())?;

    let link = rerout
        .links()
        .create(
            &CreateLinkInput::new("https://example.com/q4-sale")
                .with_domain_hostname("go.brand.com")
                .with_code("q4"),
        )
        .await?;

    println!("{}", link.short_url); // https://go.brand.com/q4

    let stats = rerout.project().stats(7).await?;
    println!(
        "Last 7 days: {} clicks, {} QR scans",
        stats.total_clicks, stats.qr_scans,
    );

    Ok(())
}
```

## API

### Construction

Build a client through the builder. Only the API key is required.

```rust,no_run
use std::time::Duration;
use rerout::Rerout;

# fn main() -> Result<(), rerout::ReroutError> {
// Defaults: production base URL, 30s timeout.
let rerout = Rerout::new("rrk_live_xxx")?;

// Or customise via the builder.
let rerout = Rerout::builder("rrk_live_xxx")
    .base_url("https://api.staging.rerout.co")? // trailing slashes trimmed
    .timeout(Duration::from_secs(15))
    .user_agent("my-app/1.0")
    .build()?;
# let _ = rerout;
# Ok(())
# }
```

### Links

```rust,no_run
# use rerout::{CreateLinkInput, ListLinksParams, Rerout, UpdateLinkInput};
# async fn run(rerout: Rerout) -> Result<(), rerout::ReroutError> {
let created = rerout.links().create(&CreateLinkInput::new("https://example.com")).await?;
let page = rerout.links().list(ListLinksParams { cursor: None, limit: Some(20) }).await?;
let one = rerout.links().get("q4").await?;
let patched = rerout.links()
    .update("q4", &UpdateLinkInput::new().set_is_active(false))
    .await?;
let removed = rerout.links().delete("q4").await?;
let stats = rerout.links().stats("q4", 30).await?;
# let _ = (created, page, one, patched, removed, stats);
# Ok(())
# }
```

`UpdateLinkInput` distinguishes "leave the field alone" from "clear it on the
server". Use the `set_*` builders to assign a value and the `clear_*` builders
to send an explicit JSON `null`:

```rust
use rerout::UpdateLinkInput;

let input = UpdateLinkInput::new()
    .set_target_url("https://example.com/new")
    .clear_expires_at(); // sends `expires_at: null`
```

An empty `UpdateLinkInput` is rejected client-side — `links().update` returns a
`bad_request` configuration error without hitting the API.

Each returned `Link` carries a read-only `tags` field — a `Vec<Tag>` where every
`Tag` has an `id`, `name`, and `color`. It is empty on `create` and populated by
`get`, `list`, and `update`. Tag writes are ignored for API-key clients.

### Project

```rust,no_run
# use rerout::Rerout;
# async fn run(rerout: Rerout) -> Result<(), rerout::ReroutError> {
let stats = rerout.project().stats(30).await?;
let project = rerout.project().me().await?;
# let _ = (stats, project);
# Ok(())
# }
```

### QR codes

`qr().url(...)` is a pure builder — it does not call the API. `qr().svg(...)`
fetches the rendered SVG with the bearer token attached.

```rust,no_run
# use rerout::{QrEcc, QrOptions, QrRefresh, Rerout};
# async fn run(rerout: Rerout) -> Result<(), rerout::ReroutError> {
let url = rerout.qr().url(
    "q4",
    &QrOptions::new()
        .with_size(12)
        .with_ecc(QrEcc::H)
        .with_domain("go.brand.com")
        .with_refresh(QrRefresh::On),
)?;

let svg = rerout.qr().svg("q4", &QrOptions::new()).await?;
# let _ = (url, svg);
# Ok(())
# }
```

### Webhook management

Manage the endpoints that receive deliveries via `webhooks()`. This is the
API-key-authenticated surface under `/v1/projects/me/webhooks` — distinct from
the inbound signature verification below.

```rust,no_run
# use rerout::{CreateWebhookInput, Rerout};
# async fn run(rerout: Rerout) -> Result<(), rerout::ReroutError> {
let created = rerout
    .webhooks()
    .create(&CreateWebhookInput::new(
        "Order events",
        "https://hooks.brand.com/rerout",
        ["link.created", "link.clicked"],
    ))
    .await?;

// The signing secret is shown once — persist it now to verify deliveries.
println!("secret: {}", created.signing_secret);
let endpoint_id = created.endpoint.id;

let list = rerout.webhooks().list().await?;
println!("{} endpoint(s)", list.endpoints.len());

let removed = rerout.webhooks().delete(&endpoint_id).await?;
assert!(removed.deleted);
# Ok(())
# }
```

### Webhook signature verification

Verify incoming webhook deliveries with the standalone helper in
`rerout::webhooks`. It performs a constant-time HMAC-SHA256 comparison and
rejects stale timestamps.

```rust
use rerout::webhooks::{verify_rerout_signature, DEFAULT_TOLERANCE_SECONDS};

# let raw_body = "{}";
# let signature_header = "t=1,v1=00";
# let secret = "whsec_xxx";
let ok = verify_rerout_signature(
    raw_body,
    signature_header,           // value of the `X-Rerout-Signature` header
    secret,                     // endpoint signing secret (`whsec_…`)
    DEFAULT_TOLERANCE_SECONDS,  // 300s window; pass 0 to disable the clock check
    None,                       // injectable clock — `None` reads the system time
);
if !ok {
    // reject the delivery
}
```

## Error handling

Every fallible method returns `Result<T, ReroutError>`. Match on the variant for
branching, or read `code()` for the stable string identifier:

```rust,no_run
# use rerout::{CreateLinkInput, Rerout, ReroutError};
# async fn run(rerout: Rerout) {
match rerout.links().create(&CreateLinkInput::new("http://insecure.example")).await {
    Ok(link) => println!("{}", link.short_url),
    Err(err) => {
        eprintln!("{} (HTTP {}): {}", err.code(), err.status(), err.message());
        if err.is_rate_limited() { /* back off and retry */ }
        if err.is_server_error() { /* transient — retry later */ }
        if let ReroutError::Api { .. } = err { /* server replied non-2xx */ }
    }
}
# }
```

Synthetic codes are used when the server did not return a JSON body:
`network_error`, `timeout`, `unexpected_response`, `decode_error`,
`unauthorized`, `forbidden`, `not_found`, `rate_limited`, `server_error`,
`client_error`.

## Local development

```bash
cargo fmt --all
cargo clippy --all-targets -- -D warnings
cargo test
cargo doc --no-deps
```

## License

MIT — see [LICENSE](../LICENSE).

## Links

- Repository: <https://github.com/ModestNerds-Co/rerout-sdks>
- API docs: <https://rerout.co>
