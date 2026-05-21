# rerout

Official Python SDK for the [Rerout](https://rerout.co) API.

Branded link infrastructure on Cloudflare. Create short links, render QR
codes, read analytics, and verify webhook signatures.

## Install

```bash
pip install rerout
# or
uv add rerout
# or
poetry add rerout
```

Requires Python 3.10+.

## Usage

```python
from rerout import Rerout, CreateLinkInput

rerout = Rerout(api_key="rrk_...")

link = rerout.links.create(
    CreateLinkInput(
        target_url="https://example.com/q4-sale",
        domain_hostname="go.brand.com",
        code="q4",
    )
)
print(link.short_url)  # https://go.brand.com/q4

stats = rerout.project.stats(days=7)
print(f"Last 7 days: {stats.total_clicks} clicks, {stats.qr_scans} QR scans")
```

The client can be used as a context manager to ensure the underlying HTTP
connection pool is closed:

```python
with Rerout(api_key="rrk_...") as rerout:
    link = rerout.links.get("q4")
```

## API

### Construction

```python
from rerout import Rerout

rerout = Rerout(
    api_key="rrk_...",                  # required
    base_url="https://api.rerout.co",   # optional, defaults shown
    timeout=30.0,                       # optional, seconds
    client=None,                        # optional, inject your own httpx.Client
)
```

Inject a custom `httpx.Client` for proxies, retries, or shared connection
pools. The SDK does not close clients it didn't create.

### Links

```python
from rerout import CreateLinkInput, UpdateLinkInput

rerout.links.create(CreateLinkInput(target_url=..., domain_hostname=..., code=...))
rerout.links.list(cursor=None, limit=None)
rerout.links.get(code)
rerout.links.update(code, UpdateLinkInput(target_url=...))
rerout.links.delete(code)
rerout.links.stats(code, days=30)
```

`UpdateLinkInput` uses an `UNSET` sentinel to distinguish "leave this field
alone" from "clear this field on the server" — pass `None` to clear, or
just don't pass the field at all:

```python
from rerout import UpdateLinkInput

# Clear the expiry, leave everything else untouched
rerout.links.update("q4", UpdateLinkInput(expires_at=None))

# Disable the link without touching SEO
rerout.links.update("q4", UpdateLinkInput(is_active=False))
```

Calling `update` with an `UpdateLinkInput` where no field has been set
raises `ReroutError(code='bad_request')` without hitting the API.

### Project

```python
rerout.project.stats(days=30)  # -> ProjectStats
rerout.project.me()            # -> ProjectInfo
```

### QR codes

```python
from rerout import QrOptions

# Pure URL builder — no network call
rerout.qr.url("q4", QrOptions(size=12, ecc="H", domain="go.brand.com"))

# Fetches the rendered SVG with the bearer token attached
svg = rerout.qr.svg("q4", QrOptions(refresh=True))
```

QR options:

| Field     | Type                       | Notes                                                                 |
| --------- | -------------------------- | --------------------------------------------------------------------- |
| `size`    | `int`                      | Module size in px. 1–32. Server default 8.                            |
| `margin`  | `int`                      | Quiet zone in modules. 0–16. Server default 4.                        |
| `ecc`     | `'L' \| 'M' \| 'Q' \| 'H'` | Error correction level.                                               |
| `domain`  | `str`                      | Force the QR to encode a specific verified custom domain.             |
| `refresh` | `str \| bool`              | Cache-bust on regenerate. `True` is sent as `1`; strings pass through. |

### Webhook signature verification

```python
from rerout import verify_rerout_signature

ok = verify_rerout_signature(
    raw_body=request.body.decode(),
    signature_header=request.headers["x-rerout-signature"],
    secret=settings.REROUT_WEBHOOK_SECRET,
)
if not ok:
    return Response(status=401)
```

Defaults to a 5-minute timestamp tolerance; pass `tolerance_seconds=0` to
disable that check.

## Error handling

Every method raises `ReroutError` on failure:

```python
from rerout import Rerout, ReroutError, CreateLinkInput

try:
    rerout.links.create(CreateLinkInput(target_url="http://insecure.example"))
except ReroutError as err:
    print(err.code)     # 'bad_target_url'
    print(err.status)   # 400
    print(err.message)  # 'target_url must use https.'
    if err.is_rate_limited:
        ...  # back off
```

Synthetic codes when the server didn't return a JSON body:
`network_error`, `timeout`, `unexpected_response`, `unauthorized`,
`forbidden`, `not_found`, `rate_limited`, `server_error`, `client_error`,
`missing_api_key`, `bad_request`.

## Local development

```bash
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
pytest
mypy src
ruff check src tests
ruff format --check src tests
```

## License

MIT — see [LICENSE](../LICENSE) in the workspace root.

## Repository

<https://github.com/ModestNerds-Co/rerout-sdks>
