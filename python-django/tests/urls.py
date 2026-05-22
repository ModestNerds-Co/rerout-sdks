"""Root URLConf used by the test suite.

Mounts the bundled ``rerout_django`` URLConf so the webhook endpoint is
reachable at ``/rerout/webhook/`` and exposes ``WebhookView`` directly at a
second path to exercise standalone wiring.
"""

from __future__ import annotations

from django.urls import include, path

from rerout_django import WebhookView

urlpatterns = [
    path("rerout/", include("rerout_django.urls")),
    path("hooks/direct/", WebhookView.as_view(), name="direct_webhook"),
]
