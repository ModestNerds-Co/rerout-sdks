# rerout/laravel

Laravel integration for the [Rerout](https://rerout.co) API.

Wraps [`rerout/sdk`](https://packagist.org/packages/rerout/sdk) with a service
provider, facade, config file, a webhook controller, and Laravel events — so
short links, QR codes, analytics, and webhooks feel native in a Laravel app.

## Install

```bash
composer require rerout/laravel
```

Requires PHP 8.2+ and Laravel 10, 11, or 12. The package auto-registers its
service provider and the `Rerout` facade via Laravel package discovery.

Publish the config file if you want to tweak defaults:

```bash
php artisan vendor:publish --tag=rerout-config
```

## Configuration

Set these in your `.env`:

```dotenv
REROUT_API_KEY=rrk_…
REROUT_WEBHOOK_SECRET=whsec_…
REROUT_BASE_URL=https://api.rerout.co   # optional
REROUT_TIMEOUT=30                       # optional, seconds
```

The published `config/rerout.php` also exposes the webhook tolerance window
and route settings — see the file's inline comments.

## Usage

### The client

The service provider binds a shared `Rerout\Rerout` singleton. Resolve it any
way Laravel allows — constructor injection, the container, or the facade.

```php
use Rerout\Laravel\Facades\Rerout;
use Rerout\Models\CreateLinkInput;

$link = Rerout::links()->create(new CreateLinkInput(
    targetUrl: 'https://example.com/q4-sale',
    domainHostname: 'go.brand.com',
    code: 'q4',
));

echo $link->shortUrl; // https://go.brand.com/q4
```

Constructor injection works too:

```php
use Rerout\Rerout;

public function __construct(private readonly Rerout $rerout) {}
```

The full client surface — `links()`, `project()`, `qr()` — is documented in
the [base SDK README](https://packagist.org/packages/rerout/sdk).

### Verifying the API key

```bash
php artisan rerout:ping
```

Calls `GET /v1/projects/me` and prints the resolved project. Exits non-zero on
any API failure — handy for deploy smoke tests.

### Webhooks

The package registers a `POST` route (default `rerout/webhook`, named
`rerout.webhook`) backed by a controller that:

1. verifies the `X-Rerout-Signature` header against `REROUT_WEBHOOK_SECRET`,
2. returns **401** on a missing/invalid/expired signature,
3. returns **400** on a body that is not a JSON object,
4. dispatches a Laravel event and returns **200** otherwise.

Point your Rerout webhook endpoint at `https://your-app.test/rerout/webhook`.

Disable the bundled route (to mount the controller yourself) by setting
`REROUT_WEBHOOK_ROUTE_ENABLED=false`, or change the path with
`REROUT_WEBHOOK_ROUTE_PATH`.

Listen for the events:

| Webhook `type` | Event |
|---|---|
| `link.clicked` | `Rerout\Laravel\Events\LinkClicked` |
| `qr.scanned` | `Rerout\Laravel\Events\QrScanned` |
| `domain.failed` | `Rerout\Laravel\Events\DomainFailed` |

```php
use Illuminate\Support\Facades\Event;
use Rerout\Laravel\Events\LinkClicked;

Event::listen(LinkClicked::class, function (LinkClicked $event) {
    // $event->payload is the decoded webhook body (array<string, mixed>)
    logger()->info('Link clicked', $event->payload);
});
```

Each event carries the full decoded payload in its readonly `$payload`
property. Unknown event types still return 200 but dispatch nothing.

### Error handling

Every client call throws `Rerout\Exceptions\ReroutException` on failure, with
a stable `code()`, HTTP `status()`, and `isRateLimited()` / `isServerError()`
flags. See the [base SDK README](https://packagist.org/packages/rerout/sdk)
for the full list of error codes.

## Local development

```bash
composer install
composer test      # PHPUnit + Orchestra Testbench
composer analyse   # PHPStan, level 9
composer format    # php-cs-fixer
```

The `composer.json` includes a path repository pointing at `../php`, so the
package builds against the local `rerout/sdk` checkout inside this workspace.

## License

MIT — see [LICENSE](../LICENSE).

## Links

- [Rerout](https://rerout.co)
- [Base PHP SDK — `rerout/sdk`](https://packagist.org/packages/rerout/sdk)
- [Workspace repository](https://github.com/ModestNerds-Co/rerout-sdks)
