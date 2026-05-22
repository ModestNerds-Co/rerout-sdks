"""The Rerout webhook receiver view.

:class:`WebhookView` verifies the ``X-Rerout-Signature`` header against the
configured ``WEBHOOK_SECRET`` using the base SDK's
:func:`rerout.verify_rerout_signature`, parses the JSON body, and dispatches
the matching Django signal.

Responses:

- ``200`` — signature valid, body parsed, signals dispatched.
- ``401`` — signature missing, malformed, or invalid.
- ``400`` — signature valid but the body is not a JSON object.
"""

from __future__ import annotations

import json
from typing import Any

from django.http import HttpRequest, HttpResponse, JsonResponse
from django.utils.decorators import method_decorator
from django.views import View
from django.views.decorators.csrf import csrf_exempt
from rerout import verify_rerout_signature

from .conf import get_signature_tolerance_seconds, get_webhook_secret
from .signals import EVENT_SIGNALS, rerout_webhook_received

SIGNATURE_HEADER = "X-Rerout-Signature"
# Django exposes request headers on ``META`` with this mangled key.
_SIGNATURE_META_KEY = "HTTP_X_REROUT_SIGNATURE"


@method_decorator(csrf_exempt, name="dispatch")
class WebhookView(View):
    """CSRF-exempt endpoint that ingests signed Rerout webhooks.

    Wire it up in ``urls.py``::

        from rerout_django import WebhookView

        urlpatterns = [
            path("webhooks/rerout/", WebhookView.as_view()),
        ]

    Or include the bundled URLConf::

        path("rerout/", include("rerout_django.urls")),

    Only ``POST`` is accepted; every other verb returns ``405`` via the
    standard :class:`~django.views.View` machinery.

    To react to events, subscribe to the signals in
    :mod:`rerout_django.signals` — do not subclass this view just to add
    handlers.
    """

    # Overrides the base ``View`` attribute; not a ``ClassVar`` because Django
    # declares it as an instance variable.
    http_method_names = ["post"]  # noqa: RUF012

    def post(self, request: HttpRequest, *args: Any, **kwargs: Any) -> HttpResponse:
        """Verify, parse, and dispatch a single webhook delivery."""
        raw_body = request.body.decode("utf-8", errors="replace")
        signature = request.META.get(_SIGNATURE_META_KEY, "")

        secret = get_webhook_secret()
        tolerance = get_signature_tolerance_seconds()

        is_valid = verify_rerout_signature(
            raw_body,
            signature,
            secret,
            tolerance_seconds=tolerance,
        )
        if not is_valid:
            return JsonResponse(
                {"detail": "invalid signature"},
                status=401,
            )

        try:
            payload = json.loads(raw_body) if raw_body else None
        except ValueError:
            return JsonResponse(
                {"detail": "body is not valid JSON"},
                status=400,
            )

        if not isinstance(payload, dict):
            return JsonResponse(
                {"detail": "body must be a JSON object"},
                status=400,
            )

        event = payload.get("event")
        event_name = event if isinstance(event, str) else ""

        # Always fire the catch-all signal.
        rerout_webhook_received.send(
            sender=self.__class__,
            event=event_name,
            payload=payload,
            request=request,
        )

        # Fire the type-specific signal when the event is recognised.
        specific = EVENT_SIGNALS.get(event_name)
        if specific is not None:
            specific.send(
                sender=self.__class__,
                event=event_name,
                payload=payload,
                request=request,
            )

        return JsonResponse({"received": True, "event": event_name}, status=200)
