# rerout/sdk

Official PHP SDK for the [Rerout](https://rerout.co) API.

Branded link infrastructure on Cloudflare — create short links, render QR
codes, read analytics, and verify webhook signatures.

## Install

```bash
composer require rerout/sdk
```

Requires PHP 8.2+ and the `json` and `hash` extensions. HTTP transport is
[Guzzle 7](https://docs.guzzlephp.org/).

## Usage

```php
use Rerout\Rerout;
use Rerout\Models\CreateLinkInput;

$rerout = new Rerout(getenv('REROUT_API_KEY'));

$link = $rerout->links()->create(new CreateLinkInput(
    targetUrl: 'https://example.com/q4-sale',
    domainHostname: 'go.brand.com',
    code: 'q4',
));

echo $link->shortUrl; // https://go.brand.com/q4

$stats = $rerout->project()->stats(7);
echo "Last 7 days: {$stats->totalClicks} clicks, {$stats->qrScans} QR scans";
```

## API

### Construction

```php
$rerout = new Rerout('rrk_…', [
    'base_url' => 'https://api.rerout.co', // optional, default shown
    'timeout' => 30,                       // optional, seconds
    'client' => $guzzleClient,             // optional — inject your own ClientInterface
    'default_headers' => [                 // optional — added to every request
        'User-Agent' => 'my-app/1.0',
    ],
]);
```

A blank or missing API key throws a `ReroutException` with code
`missing_api_key`. The `base_url` has trailing slashes trimmed.

### Links

```php
use Rerout\Models\CreateLinkInput;
use Rerout\Models\UpdateLinkInput;

$rerout->links()->create(new CreateLinkInput(
    targetUrl: 'https://example.com',
    domainHostname: 'go.brand.com', // optional
    code: 'promo',                  // optional
    expiresAt: 1893456000,          // optional, unix seconds
    seoTitle: 'Big Sale',           // optional SEO overrides
));

$result = $rerout->links()->list(cursor: null, limit: 50);
foreach ($result->links as $link) {
    echo $link->shortUrl, PHP_EOL;
}
// $result->nextCursor — pass back as `cursor` for the next page

$link = $rerout->links()->get('promo');

// Read-only tags attached to the link. Empty array when none are bound.
foreach ($link->tags as $tag) {
    echo $tag->id, ' ', $tag->name, ' ', $tag->color, PHP_EOL;
}

$link = $rerout->links()->update('promo', new UpdateLinkInput(
    isActive: false,
));

$deleted = $rerout->links()->delete('promo'); // bool

$stats = $rerout->links()->stats('promo', days: 30);
```

`UpdateLinkInput` distinguishes three states per field:

- **Leave alone** — omit the argument (the default). The field is not sent.
- **Set a value** — pass a concrete value.
- **Clear server-side** — pass `UpdateLinkInput::CLEAR`, which serialises as
  explicit `null`.

```php
new UpdateLinkInput(
    targetUrl: 'https://example.com/v2',     // set
    expiresAt: UpdateLinkInput::CLEAR,        // null it on the server
    // seoTitle omitted — left untouched
);
```

Constructing an empty `UpdateLinkInput` and calling `update()` throws a
`ReroutException` (code `bad_request`) client-side — the request never leaves
your process.

### Project

```php
$stats = $rerout->project()->stats(days: 30);
echo $stats->totalClicks, ' ', $stats->qrScans;

$me = $rerout->project()->me(); // array{id: string, name: string, slug: string}
```

### QR codes

```php
use Rerout\Models\QrOptions;

// Pure URL builder — no network call.
$url = $rerout->qr()->url('promo', new QrOptions(
    size: 12,
    margin: 2,
    ecc: 'H',
    domain: 'go.brand.com',
    refresh: true, // true → `refresh=1`; any string is forwarded verbatim
));

// Fetch the rendered SVG (sends the bearer token).
$svg = $rerout->qr()->svg('promo', new QrOptions(size: 8));
```

### Webhook endpoint management

Manage the project's webhook endpoints with an API key. The project is resolved
from the key — there is no project id in the path. The `signingSecret` is
returned **once** on create; store it to verify deliveries.

```php
use Rerout\Models\CreateWebhookInput;

// Create an endpoint.
$created = $rerout->webhooks()->create(new CreateWebhookInput(
    name: 'Order events',
    url: 'https://example.com/hooks/rerout',
    events: ['link.created', 'link.clicked'],
    isActive: true,        // optional, default true
    payloadFormat: 'json', // optional, 'json' | 'slack'
));
echo $created->endpoint->id;       // wh_…
echo $created->signingSecret;      // whsec_… — shown once, store it now

// List endpoints + every event type the server can deliver.
$result = $rerout->webhooks()->list();
foreach ($result->endpoints as $endpoint) {
    echo $endpoint->url, PHP_EOL;
}
$allEventTypes = $result->eventTypes; // list<string>

// Delete an endpoint (idempotent).
$deleted = $rerout->webhooks()->delete('wh_abc123'); // bool
```

### Webhook signature verification

Rerout signs every webhook delivery with an `X-Rerout-Signature` header in the
form `t=<unix>,v1=<hex_hmac_sha256>`. Verify it before trusting the payload:

```php
use Rerout\Webhooks\SignatureVerifier;

$ok = SignatureVerifier::verify(
    rawBody: file_get_contents('php://input'),
    signatureHeader: $_SERVER['HTTP_X_REROUT_SIGNATURE'] ?? '',
    secret: getenv('REROUT_WEBHOOK_SECRET'),
);

if (!$ok) {
    http_response_code(401);
    exit;
}
```

The HMAC is SHA-256 over `"<timestamp>.<raw_body>"` and is compared in constant
time. The default timestamp tolerance is 300 seconds — pass
`toleranceSeconds: 0` to disable the staleness check, or a custom value to
widen it. A `$clock` callable can be injected for testing.

### Error handling

Every API call throws `Rerout\Exceptions\ReroutException` on failure:

```php
use Rerout\Exceptions\ReroutException;

try {
    $rerout->links()->create(new CreateLinkInput(targetUrl: 'http://insecure'));
} catch (ReroutException $e) {
    echo $e->code();        // 'bad_target_url'
    echo $e->status();      // 400
    echo $e->getMessage();  // 'target_url must use https.'
    echo $e->path ?? '';    // the API path that failed
    if ($e->isRateLimited()) { /* back off */ }
    if ($e->isServerError()) { /* retry later */ }
}
```

When the server returns a JSON error body, `code` and `message` are taken
verbatim. When it does not, a synthetic code is used:

| Condition | Code |
|---|---|
| HTTP 401 | `unauthorized` |
| HTTP 403 | `forbidden` |
| HTTP 404 | `not_found` |
| HTTP 429 | `rate_limited` |
| HTTP 5xx | `server_error` |
| other 4xx | `client_error` |
| connection failure | `network_error` |
| timeout | `timeout` |
| 2xx non-JSON body | `unexpected_response` |

## Local development

```bash
composer install
composer test      # PHPUnit
composer analyse   # PHPStan, level 9
composer format    # php-cs-fixer
```

## License

MIT — see [LICENSE](../LICENSE).

## Links

- [Rerout](https://rerout.co)
- [Workspace repository](https://github.com/ModestNerds-Co/rerout-sdks)
