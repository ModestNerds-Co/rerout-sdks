# rerout-rails

Official Rails integration for the [Rerout](https://rerout.co) API.

Wraps the base [`rerout`](https://rubygems.org/gems/rerout) gem with
Rails-native ergonomics: a cached, initializer-driven API client, an install
generator, and a webhook controller that verifies signatures and dispatches
each delivery through `ActiveSupport::Notifications`.

## Install

Add to your `Gemfile`:

```ruby
gem 'rerout-rails'
```

Then run:

```bash
bundle install
bin/rails generate rerout:install
```

Requires Ruby 3.0+ and Rails 7.0+. The `rerout` gem is pulled in
automatically.

The generator drops `config/initializers/rerout.rb` and mounts the webhook
route in `config/routes.rb`.

## Configuration

`rails generate rerout:install` writes `config/initializers/rerout.rb`:

```ruby
Rerout::Rails.configure do |config|
  config.api_key = ENV.fetch('REROUT_API_KEY', nil)
  config.webhook_secret = ENV.fetch('REROUT_WEBHOOK_SECRET', nil)

  # config.base_url = 'https://api.rerout.co'
  # config.timeout = 30
  # config.signature_tolerance_seconds = 300
end
```

`api_key`, `webhook_secret`, and `base_url` fall back to the `REROUT_API_KEY`,
`REROUT_WEBHOOK_SECRET`, and `REROUT_BASE_URL` environment variables when not
set explicitly. Keep secrets out of source control — prefer Rails encrypted
credentials or environment variables.

## API client

`Rerout::Rails.client` is a process-wide, lazily-built `Rerout::Client`. It is
created once from your configuration and reused across requests — the
underlying Faraday connection is thread-safe, so sharing one instance is the
recommended pattern.

```ruby
class LinksController < ApplicationController
  def create
    link = Rerout::Rails.client.links.create(
      Rerout::CreateLinkInput.new(target_url: params[:target_url])
    )
    render json: { short_url: link.short_url }
  end
end
```

`Rerout::Rails.client` raises `Rerout::Rails::ConfigurationError` if `api_key`
is unset. See the [`rerout` gem README](https://rubygems.org/gems/rerout) for
the full client surface — `links`, `project`, and `qr` namespaces.

## Webhooks

The install generator mounts the receiver:

```ruby
# config/routes.rb
post '/rerout/webhooks', to: 'rerout/rails/webhook#receive', as: :rerout_webhook
```

Point your Rerout dashboard webhook endpoint at `POST /rerout/webhooks`. The
controller:

- verifies the `X-Rerout-Signature` header against `webhook_secret` with a
  constant-time HMAC check (CSRF protection is skipped — webhooks are
  server-to-server),
- responds `200` when the signature is valid and the body is a JSON object,
- responds `401` when the signature is missing, malformed, stale, or wrong,
- responds `400` when the body is not a JSON object.

To wire it up manually instead of using the generator, add the route above to
`config/routes.rb` yourself.

## Reacting to events

Every verified delivery is instrumented through
`ActiveSupport::Notifications`. Subscribe anywhere — an initializer, an
`ApplicationJob`, a service object:

```ruby
# Fires for every verified webhook, regardless of event type.
ActiveSupport::Notifications.subscribe('rerout.webhook') do |event|
  Rails.logger.info("Rerout webhook: #{event.payload[:event]}")
end

# Fires only for link.clicked events.
ActiveSupport::Notifications.subscribe('rerout.link.clicked') do |event|
  code = event.payload[:body]['code']
  ClickCounter.increment(code)
end
```

The instrumentation payload carries:

- `:event` — the event-type string (e.g. `"link.clicked"`), or `""`.
- `:body` — the full parsed JSON body as a Hash.
- `:request` — the `ActionDispatch::Request` that delivered the webhook.

Topics emitted: `rerout.webhook` (catch-all) plus `rerout.link.created`,
`rerout.link.updated`, `rerout.link.deleted`, `rerout.link.clicked`, and
`rerout.qr.scanned` for recognised events.

## Webhook signature verification

The controller verifies signatures for you. To verify a signature elsewhere —
in a background job, a Rack endpoint, a custom controller — use the base SDK
helper directly:

```ruby
ok = Rerout::Webhooks.verify_signature(
  raw_body: request.raw_post,
  signature_header: request.headers['X-Rerout-Signature'],
  secret: Rerout::Rails.config.webhook_secret!
)
```

The five-minute timestamp tolerance is configurable via
`config.signature_tolerance_seconds` (`0` disables the staleness check).

## Error handling

API calls through `Rerout::Rails.client` raise `Rerout::Error`, with a stable
`code`, an HTTP `status`, and `rate_limited?` / `server_error?` flags:

```ruby
begin
  Rerout::Rails.client.links.get(params[:code])
rescue Rerout::Error => e
  return head(:not_found) if e.code == 'not_found'
  raise
end
```

Misconfiguration (missing `api_key` or `webhook_secret`) raises
`Rerout::Rails::ConfigurationError`.

## License

MIT — see [LICENSE](LICENSE), a copy of the workspace
[LICENSE](https://github.com/ModestNerds-Co/rerout-sdks/blob/main/LICENSE).

## Links

- API docs: <https://rerout.co/docs>
- Source: <https://github.com/ModestNerds-Co/rerout-sdks>
