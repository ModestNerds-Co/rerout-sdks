"""Pytest + Django bootstrap for the rerout-django test suite.

Django has to be configured *before* any ``django.*`` import that touches
settings. Pytest imports ``conftest.py`` before collecting test modules, so
configuring here — at import time — is the reliable hook.

No ``DJANGO_SETTINGS_MODULE`` / settings file is needed: everything runs from
an in-memory ``settings.configure(...)`` call.
"""

from __future__ import annotations

import hashlib
import hmac

import django
from django.conf import settings

TEST_API_KEY = "rrk_test_key"
TEST_WEBHOOK_SECRET = "whsec_test_secret"


def _configure_django() -> None:
    if settings.configured:
        return
    settings.configure(
        DEBUG=True,
        SECRET_KEY="test-secret-key-not-for-production",
        ALLOWED_HOSTS=["*"],
        INSTALLED_APPS=[
            "django.contrib.contenttypes",
            "django.contrib.auth",
            "rerout_django",
        ],
        DATABASES={
            "default": {
                "ENGINE": "django.db.backends.sqlite3",
                "NAME": ":memory:",
            }
        },
        ROOT_URLCONF="tests.urls",
        MIDDLEWARE=[
            "django.middleware.common.CommonMiddleware",
            "django.middleware.csrf.CsrfViewMiddleware",
        ],
        USE_TZ=True,
        REROUT={
            "API_KEY": TEST_API_KEY,
            "WEBHOOK_SECRET": TEST_WEBHOOK_SECRET,
        },
    )
    django.setup()


_configure_django()


def sign_body(body: str, secret: str, timestamp: int) -> str:
    """Build a valid ``t=…,v1=…`` Rerout signature header.

    Mirrors the server-side scheme: HMAC-SHA256 over ``"{ts}.{body}"``.
    """
    mac = hmac.new(
        secret.encode("utf-8"),
        f"{timestamp}.{body}".encode(),
        hashlib.sha256,
    ).hexdigest()
    return f"t={timestamp},v1={mac}"
