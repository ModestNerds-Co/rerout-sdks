"""Settings access + a process-wide cached Rerout client.

Configuration lives under a single ``REROUT`` dict in Django settings::

    REROUT = {
        "API_KEY": "rrk_...",            # required for get_rerout_client()
        "WEBHOOK_SECRET": "whsec_...",   # required for WebhookView
        "BASE_URL": "https://api.rerout.co",   # optional
        "TIMEOUT": 30.0,                       # optional, seconds
        "SIGNATURE_TOLERANCE_SECONDS": 300,    # optional
    }
"""

from __future__ import annotations

import threading
from typing import Any

from django.core.exceptions import ImproperlyConfigured
from rerout import DEFAULT_SIGNATURE_TOLERANCE_SECONDS, Rerout

SETTINGS_KEY = "REROUT"

_DEFAULTS: dict[str, Any] = {
    "API_KEY": None,
    "WEBHOOK_SECRET": None,
    "BASE_URL": None,
    "TIMEOUT": 30.0,
    "SIGNATURE_TOLERANCE_SECONDS": DEFAULT_SIGNATURE_TOLERANCE_SECONDS,
}

_client_lock = threading.Lock()
_client: Rerout | None = None


def get_rerout_settings() -> dict[str, Any]:
    """Return the resolved ``REROUT`` settings dict, merged over defaults.

    Reading settings lazily (rather than at import time) keeps the package
    compatible with ``django.test.override_settings`` and with apps that
    configure settings after importing ``rerout_django``.

    Raises:
        ImproperlyConfigured: If ``settings.REROUT`` exists but is not a dict.
    """
    # Imported here so the module is importable before Django settings are
    # configured (e.g. during ``setup.py``/packaging introspection).
    from django.conf import settings

    raw = getattr(settings, SETTINGS_KEY, {})
    if not isinstance(raw, dict):
        raise ImproperlyConfigured(
            f"settings.{SETTINGS_KEY} must be a dict, got {type(raw).__name__}."
        )
    merged = dict(_DEFAULTS)
    merged.update(raw)
    return merged


def get_rerout_client() -> Rerout:
    """Return a process-wide cached :class:`rerout.Rerout` instance.

    The client is built once from ``settings.REROUT["API_KEY"]`` and reused
    across requests — :class:`rerout.Rerout` wraps a thread-safe
    :class:`httpx.Client`, so sharing one instance is both safe and the
    recommended pattern.

    Raises:
        ImproperlyConfigured: If ``REROUT["API_KEY"]`` is missing or blank.
    """
    global _client
    if _client is not None:
        return _client

    with _client_lock:
        if _client is not None:
            return _client

        config = get_rerout_settings()
        api_key = config.get("API_KEY")
        if not api_key or not isinstance(api_key, str):
            raise ImproperlyConfigured(
                f"settings.{SETTINGS_KEY}['API_KEY'] is required to build a "
                "Rerout client."
            )

        kwargs: dict[str, Any] = {"api_key": api_key}
        base_url = config.get("BASE_URL")
        if base_url:
            kwargs["base_url"] = base_url
        timeout = config.get("TIMEOUT")
        if timeout is not None:
            kwargs["timeout"] = timeout

        _client = Rerout(**kwargs)
        return _client


def reset_rerout_client() -> None:
    """Drop the cached client and close it if one exists.

    Mainly useful in tests that swap ``REROUT`` settings between cases — call
    this so the next :func:`get_rerout_client` rebuilds against fresh config.
    """
    global _client
    with _client_lock:
        if _client is not None:
            _client.close()
            _client = None


def get_webhook_secret() -> str:
    """Return the configured webhook signing secret.

    Raises:
        ImproperlyConfigured: If ``REROUT["WEBHOOK_SECRET"]`` is missing/blank.
    """
    secret = get_rerout_settings().get("WEBHOOK_SECRET")
    if not secret or not isinstance(secret, str):
        raise ImproperlyConfigured(
            f"settings.{SETTINGS_KEY}['WEBHOOK_SECRET'] is required to verify "
            "Rerout webhook signatures."
        )
    return secret


def get_signature_tolerance_seconds() -> int:
    """Return the configured signature timestamp tolerance window in seconds."""
    value = get_rerout_settings().get("SIGNATURE_TOLERANCE_SECONDS")
    return int(value) if value is not None else DEFAULT_SIGNATURE_TOLERANCE_SECONDS
