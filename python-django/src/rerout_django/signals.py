"""Django signals dispatched by :class:`rerout_django.views.WebhookView`.

Every verified webhook fires :data:`rerout_webhook_received`. In addition,
events with a recognised ``event`` field fire a dedicated, more specific
signal so receivers can subscribe narrowly.

All signals are sent with these keyword arguments:

- ``sender`` — the :class:`~rerout_django.views.WebhookView` class.
- ``event`` — the event type string (e.g. ``"link.clicked"``).
- ``payload`` — the full parsed JSON body as a ``dict``.
- ``request`` — the :class:`~django.http.HttpRequest` that delivered it.

Example::

    from django.dispatch import receiver
    from rerout_django import rerout_link_clicked

    @receiver(rerout_link_clicked)
    def on_click(sender, event, payload, request, **kwargs):
        print(payload["code"], "was clicked")
"""

from __future__ import annotations

from django.dispatch import Signal

rerout_webhook_received = Signal()
"""Fired for every successfully verified webhook, regardless of event type."""

rerout_link_created = Signal()
"""Fired for ``link.created`` events."""

rerout_link_updated = Signal()
"""Fired for ``link.updated`` events."""

rerout_link_deleted = Signal()
"""Fired for ``link.deleted`` events."""

rerout_link_clicked = Signal()
"""Fired for ``link.clicked`` events."""

rerout_qr_scanned = Signal()
"""Fired for ``qr.scanned`` events."""


# Maps the API's ``event`` string onto the matching specific signal. Events
# not in this map still fire ``rerout_webhook_received`` only.
EVENT_SIGNALS: dict[str, Signal] = {
    "link.created": rerout_link_created,
    "link.updated": rerout_link_updated,
    "link.deleted": rerout_link_deleted,
    "link.clicked": rerout_link_clicked,
    "qr.scanned": rerout_qr_scanned,
}
