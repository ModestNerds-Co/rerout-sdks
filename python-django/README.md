# rerout-django

Official [Django](https://www.djangoproject.com/) integration for the
[Rerout](https://rerout.co) API.

Wraps the base [`rerout`](https://pypi.org/project/rerout/) SDK with
Django-native ergonomics: a settings-driven, cached API client and a
signed-webhook receiver view that fans events out as Django signals.

## Install

```bash
pip install rerout-django
# or
uv add rerout-django
# or
poetry add rerout-django
```

Requires Python 3.10+ and Django 4.2 or 5.x. Installs the `rerout` base SDK
as a dependency.

## Setup

Add the app to `INSTALLED_APPS` and configure it via a single `REROUT`
settings dict:

```python
# settings.py
INSTALLED_APPS = [
    # ...
    "rerout_django",
]

REROUT = {
    "API_KEY": env("REROUT_API_KEY"),            # required for the client
    "WEBHOOK_SECRET": env("REROUT_WEBHOOK_SECRET"),  # required for webhooks
    # Optional:
    # "BASE_URL": "https://api.staging.rerout.co",
    # "TIMEOUT": 30.0,
    # "SIGNATURE_TOLERANCE_SECONDS": 300,
}
```

## Usage

### The API client

`get_rerout_client()` returns a process-wide, lazily constructed, cached
`rerout.Rerout` instance built from your `REROUT` settings. It wraps a
thread-safe `httpx.Client`, so sharing the one instance across requests is
both safe and recommended.

```python
from rerout import CreateLinkInput
from rerout_django import get_rerout_client

def create_promo(request):
    client = get_rerout_client()
    link = client.links.create(
        CreateLinkInput(target_url="https://example.com/q4-sale", code="q4")
    )
    return JsonResponse({"short_url": link.short_url})
```

Every method on the client raises `rerout.ReroutError` on failure — see the
[base SDK README](https://pypi.org/project/rerout/) for the full surface
(`links`, `project`, `qr`) and error handling.

`reset_rerout_client()` drops and closes the cached client; the next
`get_rerout_client()` call rebuilds it. This is mostly useful in tests that
swap `REROUT` settings between cases.

### Receiving webhooks

`WebhookView` is a CSRF-exempt view that verifies the `X-Rerout-Signature`
header, parses the JSON body, and dispatches Django signals. Mount the
bundled URLConf:

```python
# urls.py
from django.urls import include, path

urlpatterns = [
    path("rerout/", include("rerout_django.urls")),
]
```

That serves the endpoint at `/rerout/webhook/` (URL name `rerout_webhook`).
Prefer a different path? Wire the view directly instead:

```python
from rerout_django import WebhookView

urlpatterns = [
    path("hooks/rerout/", WebhookView.as_view()),
]
```

The view responds:

| Status | Meaning                                              |
| ------ | ---------------------------------------------------- |
| `200`  | Signature valid, body parsed, signals dispatched.    |
| `401`  | Signature missing, malformed, or invalid.            |
| `400`  | Signature valid but the body is not a JSON object.   |

Signature verification uses the base SDK's `verify_rerout_signature` with a
configurable timestamp tolerance (`REROUT["SIGNATURE_TOLERANCE_SECONDS"]`,
default 300; set `0` to disable the staleness check).

### Reacting to events with signals

Subscribe to the signals in `rerout_django.signals` — do not subclass
`WebhookView`. Every verified delivery fires `rerout_webhook_received`; events
with a recognised type also fire a dedicated signal.

```python
from django.dispatch import receiver
from rerout_django import rerout_link_clicked, rerout_webhook_received

@receiver(rerout_link_clicked)
def on_click(sender, event, payload, request, **kwargs):
    print(payload["code"], "was clicked")

@receiver(rerout_webhook_received)
def log_everything(sender, event, payload, request, **kwargs):
    print("received", event)
```

Every signal is sent with these keyword arguments:

| Argument  | Description                                          |
| --------- | ---------------------------------------------------- |
| `sender`  | The `WebhookView` class.                             |
| `event`   | The event type string (e.g. `"link.clicked"`).       |
| `payload` | The full parsed JSON body, as a `dict`.              |
| `request` | The `HttpRequest` that delivered the webhook.        |

Available signals:

| Signal                    | Event type      |
| ------------------------- | --------------- |
| `rerout_webhook_received` | _every_ event   |
| `rerout_link_created`     | `link.created`  |
| `rerout_link_updated`     | `link.updated`  |
| `rerout_link_deleted`     | `link.deleted`  |
| `rerout_link_clicked`     | `link.clicked`  |
| `rerout_qr_scanned`       | `qr.scanned`    |

### Error handling

Configuration problems raise `django.core.exceptions.ImproperlyConfigured`:

- `get_rerout_client()` — when `REROUT["API_KEY"]` is missing or blank.
- `WebhookView` — when `REROUT["WEBHOOK_SECRET"]` is missing or blank.

API call failures raise `rerout.ReroutError` (with a stable `code`, an HTTP
`status`, and convenience flags `is_rate_limited` / `is_server_error`) — see
the base SDK for details.

## Local development

```bash
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"   # pulls the sibling ../python checkout for `rerout`
pytest
mypy src
ruff check src tests
```

## License

MIT — see [LICENSE](../LICENSE) in the workspace root.

## Repository

<https://github.com/ModestNerds-Co/rerout-sdks>
