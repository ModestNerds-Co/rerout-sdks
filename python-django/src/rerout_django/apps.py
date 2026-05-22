"""Django ``AppConfig`` for the Rerout integration."""

from __future__ import annotations

from django.apps import AppConfig


class ReroutDjangoConfig(AppConfig):
    """App config — registered by adding ``"rerout_django"`` to ``INSTALLED_APPS``.

    The integration is configuration-only (no models, no migrations); this
    config exists so Django reports the app cleanly and so the label is
    stable for signal dispatch and admin tooling.
    """

    name = "rerout_django"
    label = "rerout_django"
    verbose_name = "Rerout"
    default_auto_field = "django.db.models.BigAutoField"
