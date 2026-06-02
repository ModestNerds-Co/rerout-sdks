# rerout

Official Ruby SDK for the [Rerout](https://rerout.co) API.

Branded link infrastructure on Cloudflare — create short links, render QR
codes, read analytics, and verify webhook signatures.

## Install

Add to your `Gemfile`:

```ruby
gem 'rerout'
```

Then run `bundle install`. Or install it directly:

```bash
gem install rerout
```

Requires Ruby 3.0+. Built on [Faraday](https://lostisland.github.io/faraday/)
2.x — the HTTP connection is injectable, so the same client runs against the
real API in production and a stubbed adapter in tests.

## Usage

```ruby
require 'rerout'

rerout = Rerout::Client.new(api_key: ENV.fetch('REROUT_API_KEY'))

link = rerout.links.create(
  Rerout::CreateLinkInput.new(
    target_url: 'https://example.com/q4-sale',
    domain_hostname: 'go.brand.com',
    code: 'q4'
  )
)

puts link.short_url # => https://go.brand.com/q4
```

## Construction

```ruby
# Production — only the API key is required.
rerout = Rerout::Client.new(api_key: ENV.fetch('REROUT_API_KEY'))

# Staging / self-hosted — override the base URL (trailing slashes are trimmed).
rerout = Rerout::Client.new(
  api_key: ENV.fetch('REROUT_API_KEY'),
  base_url: 'https://staging.rerout.co'
)

# Custom timeout (seconds), User-Agent, or a shared Faraday connection.
rerout = Rerout::Client.new(
  api_key: ENV.fetch('REROUT_API_KEY'),
  timeout: 10,
  user_agent: 'my-app/2.1'
)
```

A blank or missing `api_key` raises `Rerout::Error` with code `missing_api_key`
before any network call.

The client exposes three namespaces: `links`, `project`, and `qr`.

## Links

```ruby
# Create
link = rerout.links.create(
  Rerout::CreateLinkInput.new(target_url: 'https://example.com')
)

# List (paginated)
page = rerout.links.list(limit: 25)
page.links        # => [Rerout::Models::Link, ...]
page.next_cursor  # => Integer or nil
page = rerout.links.list(cursor: page.next_cursor) if page.next_cursor

# Get one
link = rerout.links.get('q4')
link.tags # => [Rerout::Models::Tag, ...] — read-only { id, name, color }

# Update — only the fields you set are sent.
rerout.links.update('q4', Rerout::UpdateLinkInput.new(is_active: false))

# Delete (soft delete)
rerout.links.delete('q4') # => { "deleted" => true }

# Per-link stats (defaults to 30 days)
stats = rerout.links.stats('q4', days: 7)
stats.total_clicks
```

### Clearing fields on update

`Rerout::UpdateLinkInput` distinguishes *"leave this field alone"* from *"set
this field to null on the server"*. Pass `Rerout::CLEAR` to null a field:

```ruby
# Sends { "expires_at": null } — removes the link's expiry.
rerout.links.update('q4', Rerout::UpdateLinkInput.new(expires_at: Rerout::CLEAR))

# Sends { "target_url": "https://new.example.com" } — leaves everything else.
rerout.links.update('q4', Rerout::UpdateLinkInput.new(target_url: 'https://new.example.com'))
```

An `UpdateLinkInput` with no fields set raises `Rerout::Error` (code
`empty_update`) client-side without hitting the API.

## Project

```ruby
# Aggregate stats across every link (defaults to 30 days).
stats = rerout.project.stats(days: 30)
stats.total_clicks
stats.daily      # => [Rerout::Models::DailyClicksPoint, ...]
stats.top_codes  # => [Rerout::Models::StatsBreakdown, ...]

# Identity of the project that owns the API key.
me = rerout.project.me
me.slug
```

## QR

`qr.url` is a pure builder — it never touches the network:

```ruby
rerout.qr.url('q4')
# => "https://api.rerout.co/v1/links/q4/qr"

rerout.qr.url('q4', Rerout::QrOptions.new(size: 12, ecc: 'H', domain: 'go.brand.com'))
# => "https://api.rerout.co/v1/links/q4/qr?size=12&ecc=H&domain=go.brand.com"
```

`qr.svg` fetches the rendered SVG from the API with the bearer token attached:

```ruby
svg = rerout.qr.svg('q4', Rerout::QrOptions.new(size: 16))
File.write('q4.svg', svg)
```

QR options: `size` (1–32), `margin` (0–16), `ecc` (`L`/`M`/`Q`/`H`), `domain`,
and `refresh` (`true` is serialized as `1`; a string is sent verbatim).

## Webhook signature verification

Rerout signs every webhook delivery with an `X-Rerout-Signature` header. Verify
it before trusting the payload:

```ruby
ok = Rerout::Webhooks.verify_signature(
  raw_body: request.raw_post,
  signature_header: request.headers['X-Rerout-Signature'],
  secret: ENV.fetch('REROUT_WEBHOOK_SECRET')
)

head(:unauthorized) and return unless ok
```

`Rerout.verify_signature` is a module-level shortcut for the same method. The
HMAC-SHA256 comparison runs in constant time, and a five-minute timestamp
tolerance guards against replay attacks. Pass `tolerance_seconds: 0` to disable
the timestamp check. The method never raises — it returns `false` for every
failure mode (malformed header, wrong secret, stale timestamp, tampered body).

## Error handling

Every failure raises `Rerout::Error`:

```ruby
begin
  rerout.links.get('does-not-exist')
rescue Rerout::Error => e
  e.code             # => "not_found" (stable string, API or synthetic)
  e.status           # => 404 (HTTP status, or 0 for network/timeout failures)
  e.message          # => human-readable description
  e.path             # => API path, when supplied by the server
  e.timestamp        # => server timestamp, when supplied
  e.rate_limited?    # => true when status == 429
  e.server_error?    # => true for HTTP 5xx
end
```

When the server responds without a JSON body the SDK fills in a synthetic
`code`: `unauthorized` (401), `forbidden` (403), `not_found` (404),
`rate_limited` (429), `server_error` (5xx), `client_error` (other 4xx),
`network_error` (connection failure), `timeout`, and `unexpected_response`
(a 2xx body that is not valid JSON).

## License

MIT — see [LICENSE](LICENSE), a copy of the workspace
[LICENSE](https://github.com/ModestNerds-Co/rerout-sdks/blob/main/LICENSE).

## Links

- API docs: <https://rerout.co/docs>
- Source: <https://github.com/ModestNerds-Co/rerout-sdks>
