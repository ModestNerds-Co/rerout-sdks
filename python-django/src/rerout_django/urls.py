"""A ready-to-include URLConf for the Rerout webhook endpoint.

Include it from your project's ``urls.py``::

    from django.urls import include, path

    urlpatterns = [
        path("rerout/", include("rerout_django.urls")),
    ]

That mounts the webhook receiver at ``/rerout/webhook/`` with the URL name
``rerout_webhook``. If you want a different path, skip this module and wire
:class:`rerout_django.views.WebhookView` directly instead.
"""

from __future__ import annotations

from django.urls import path

from .views import WebhookView

app_name = "rerout_django"

urlpatterns = [
    path("webhook/", WebhookView.as_view(), name="rerout_webhook"),
]
