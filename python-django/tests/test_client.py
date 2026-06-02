"""Tests for ``rerout_django.conf`` — settings access + cached client."""

from __future__ import annotations

import pytest
from django.core.exceptions import ImproperlyConfigured
from django.test import override_settings
from rerout import DEFAULT_SIGNATURE_TOLERANCE_SECONDS, Rerout

from rerout_django import get_rerout_client, get_rerout_settings, reset_rerout_client
from rerout_django.conf import get_signature_tolerance_seconds, get_webhook_secret


@pytest.fixture(autouse=True)
def _clean_client() -> None:
    """Reset the cached client before and after every test."""
    reset_rerout_client()
    yield
    reset_rerout_client()


# ─── get_rerout_settings ───────────────────────────────────────────────────


def test_settings_merge_over_defaults() -> None:
    config = get_rerout_settings()
    assert config["API_KEY"] == "rrk_test_key"
    assert config["WEBHOOK_SECRET"] == "whsec_test_secret"
    # Defaults filled in for keys the test settings omit.
    assert config["TIMEOUT"] == 30.0
    assert config["SIGNATURE_TOLERANCE_SECONDS"] == DEFAULT_SIGNATURE_TOLERANCE_SECONDS


@override_settings(REROUT={"API_KEY": "rrk_x", "BASE_URL": "https://api.staging.rerout.co"})
def test_settings_override_respected() -> None:
    config = get_rerout_settings()
    assert config["API_KEY"] == "rrk_x"
    assert config["BASE_URL"] == "https://api.staging.rerout.co"


@override_settings(REROUT="not-a-dict")
def test_settings_non_dict_raises() -> None:
    with pytest.raises(ImproperlyConfigured):
        get_rerout_settings()


@override_settings(REROUT={})
def test_settings_empty_dict_falls_back_to_defaults() -> None:
    config = get_rerout_settings()
    assert config["API_KEY"] is None
    assert config["WEBHOOK_SECRET"] is None
    assert config["TIMEOUT"] == 30.0


# ─── get_rerout_client ─────────────────────────────────────────────────────


def test_client_built_from_settings() -> None:
    client = get_rerout_client()
    assert isinstance(client, Rerout)


def test_client_is_cached() -> None:
    first = get_rerout_client()
    second = get_rerout_client()
    assert first is second


def test_reset_client_rebuilds() -> None:
    first = get_rerout_client()
    reset_rerout_client()
    second = get_rerout_client()
    assert first is not second


@override_settings(REROUT={"API_KEY": "rrk_custom", "BASE_URL": "https://api.staging.rerout.co"})
def test_client_honours_base_url() -> None:
    reset_rerout_client()
    client = get_rerout_client()
    assert client.base_url == "https://api.staging.rerout.co"


@override_settings(REROUT={"WEBHOOK_SECRET": "whsec_x"})
def test_client_missing_api_key_raises() -> None:
    reset_rerout_client()
    with pytest.raises(ImproperlyConfigured):
        get_rerout_client()


@override_settings(REROUT={"API_KEY": ""})
def test_client_blank_api_key_raises() -> None:
    reset_rerout_client()
    with pytest.raises(ImproperlyConfigured):
        get_rerout_client()


# ─── get_webhook_secret ────────────────────────────────────────────────────


def test_webhook_secret_returned() -> None:
    assert get_webhook_secret() == "whsec_test_secret"


@override_settings(REROUT={"API_KEY": "rrk_x"})
def test_webhook_secret_missing_raises() -> None:
    with pytest.raises(ImproperlyConfigured):
        get_webhook_secret()


@override_settings(REROUT={"API_KEY": "rrk_x", "WEBHOOK_SECRET": ""})
def test_webhook_secret_blank_raises() -> None:
    with pytest.raises(ImproperlyConfigured):
        get_webhook_secret()


# ─── get_signature_tolerance_seconds ───────────────────────────────────────


def test_tolerance_defaults_to_300() -> None:
    assert get_signature_tolerance_seconds() == DEFAULT_SIGNATURE_TOLERANCE_SECONDS


@override_settings(
    REROUT={
        "API_KEY": "rrk_x",
        "WEBHOOK_SECRET": "whsec_x",
        "SIGNATURE_TOLERANCE_SECONDS": 60,
    }
)
def test_tolerance_override_respected() -> None:
    assert get_signature_tolerance_seconds() == 60


@override_settings(
    REROUT={
        "API_KEY": "rrk_x",
        "WEBHOOK_SECRET": "whsec_x",
        "SIGNATURE_TOLERANCE_SECONDS": 0,
    }
)
def test_tolerance_zero_respected() -> None:
    assert get_signature_tolerance_seconds() == 0


# ─── package surface ───────────────────────────────────────────────────────


def test_version_exposed() -> None:
    import rerout_django

    assert rerout_django.__version__ == "0.2.0"


def test_app_config_label() -> None:
    from django.apps import apps

    config = apps.get_app_config("rerout_django")
    assert config.verbose_name == "Rerout"
