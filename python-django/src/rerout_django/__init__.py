"""Official Django integration for the Rerout branded-link API.

Wraps the base :mod:`rerout` SDK with Django-native ergonomics:

- ``get_rerout_client()`` — a process-wide :class:`rerout.Rerout` built from
  ``settings.REROUT``.
- ``WebhookView`` — a CSRF-exempt view that verifies the
  ``X-Rerout-Signature`` header and dispatches a Django signal per event type.
- Signals — ``rerout_webhook_received`` plus one per known event so apps can
  ``@receiver``-subscribe without parsing payloads themselves.

Example ``settings.py``::

    INSTALLED_APPS = [..., "rerout_django"]

    REROUT = {
        "API_KEY": env("REROUT_API_KEY"),
        "WEBHOOK_SECRET": env("REROUT_WEBHOOK_SECRET"),
    }

See https://rerout.co and
https://github.com/ModestNerds-Co/rerout-sdks for more details.
"""

from __future__ import annotations

from .conf import get_rerout_client, get_rerout_settings, reset_rerout_client
from .signals import (
    rerout_link_clicked,
    rerout_link_created,
    rerout_link_deleted,
    rerout_link_updated,
    rerout_qr_scanned,
    rerout_webhook_received,
)
from .views import WebhookView

__version__ = "0.1.0"

default_app_config = "rerout_django.apps.ReroutDjangoConfig"

__all__ = [
    "WebhookView",
    "__version__",
    "default_app_config",
    "get_rerout_client",
    "get_rerout_settings",
    "rerout_link_clicked",
    "rerout_link_created",
    "rerout_link_deleted",
    "rerout_link_updated",
    "rerout_qr_scanned",
    "rerout_webhook_received",
    "reset_rerout_client",
]
