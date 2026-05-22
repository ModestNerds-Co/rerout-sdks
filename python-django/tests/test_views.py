"""Tests for ``rerout_django.views.WebhookView`` — verify / parse / dispatch."""

from __future__ import annotations

import json
import time

import pytest
from django.test import Client, override_settings

from conftest import TEST_WEBHOOK_SECRET, sign_body

WEBHOOK_URL = "/rerout/webhook/"
DIRECT_URL = "/hooks/direct/"


@pytest.fixture
def http() -> Client:
    """A Django test client that does not enforce CSRF (the view is exempt)."""
    return Client()


def _post(
    http: Client,
    body: str,
    *,
    url: str = WEBHOOK_URL,
    signature: str | None = None,
    secret: str = TEST_WEBHOOK_SECRET,
    timestamp: int | None = None,
):  # type: ignore[no-untyped-def]
    """POST ``body`` to the webhook URL with a (by default valid) signature."""
    ts = timestamp if timestamp is not None else int(time.time())
    header = signature if signature is not None else sign_body(body, secret, ts)
    return http.post(
        url,
        data=body,
        content_type="application/json",
        HTTP_X_REROUT_SIGNATURE=header,
    )


# ─── 200 — happy path ──────────────────────────────────────────────────────


def test_valid_webhook_returns_200(http: Client) -> None:
    body = json.dumps({"event": "link.clicked", "code": "q4"})
    response = _post(http, body)
    assert response.status_code == 200
    assert response.json() == {"received": True, "event": "link.clicked"}


def test_valid_webhook_unknown_event_still_200(http: Client) -> None:
    body = json.dumps({"event": "something.new", "data": 1})
    response = _post(http, body)
    assert response.status_code == 200
    assert response.json()["event"] == "something.new"


def test_valid_webhook_no_event_field(http: Client) -> None:
    body = json.dumps({"code": "q4"})
    response = _post(http, body)
    assert response.status_code == 200
    assert response.json()["event"] == ""


def test_direct_view_wiring_works(http: Client) -> None:
    body = json.dumps({"event": "link.created"})
    response = _post(http, body, url=DIRECT_URL)
    assert response.status_code == 200


def test_view_is_csrf_exempt(http: Client) -> None:
    # A CSRF-enforcing client would 403 a POST with no token; the view is
    # decorated csrf_exempt so this still reaches signature verification.
    enforcing = Client(enforce_csrf_checks=True)
    body = json.dumps({"event": "link.clicked"})
    response = _post(enforcing, body)
    assert response.status_code == 200


# ─── 401 — bad signature ───────────────────────────────────────────────────


def test_missing_signature_returns_401(http: Client) -> None:
    body = json.dumps({"event": "link.clicked"})
    # No X-Rerout-Signature header at all.
    response = http.post(WEBHOOK_URL, data=body, content_type="application/json")
    assert response.status_code == 401


def test_empty_signature_returns_401(http: Client) -> None:
    body = json.dumps({"event": "link.clicked"})
    response = _post(http, body, signature="")
    assert response.status_code == 401


def test_garbage_signature_returns_401(http: Client) -> None:
    body = json.dumps({"event": "link.clicked"})
    response = _post(http, body, signature="totally-bogus")
    assert response.status_code == 401


def test_wrong_secret_signature_returns_401(http: Client) -> None:
    body = json.dumps({"event": "link.clicked"})
    response = _post(http, body, secret="whsec_attacker")
    assert response.status_code == 401


def test_tampered_body_returns_401(http: Client) -> None:
    body = json.dumps({"event": "link.clicked"})
    ts = int(time.time())
    header = sign_body(body, TEST_WEBHOOK_SECRET, ts)
    # Send a different body than what was signed.
    response = http.post(
        WEBHOOK_URL,
        data=body + " ",
        content_type="application/json",
        HTTP_X_REROUT_SIGNATURE=header,
    )
    assert response.status_code == 401


def test_expired_signature_returns_401(http: Client) -> None:
    body = json.dumps({"event": "link.clicked"})
    # Signed an hour ago — well outside the default 300s tolerance.
    old_ts = int(time.time()) - 3600
    response = _post(http, body, timestamp=old_ts)
    assert response.status_code == 401


def test_401_body_shape(http: Client) -> None:
    body = json.dumps({"event": "link.clicked"})
    response = _post(http, body, signature="garbage")
    assert response.json() == {"detail": "invalid signature"}


# ─── 400 — bad body ────────────────────────────────────────────────────────


def test_non_json_body_returns_400(http: Client) -> None:
    body = "this is not json"
    response = _post(http, body)
    assert response.status_code == 400
    assert response.json()["detail"] == "body is not valid JSON"


def test_json_array_body_returns_400(http: Client) -> None:
    body = json.dumps(["not", "an", "object"])
    response = _post(http, body)
    assert response.status_code == 400
    assert response.json()["detail"] == "body must be a JSON object"


def test_json_scalar_body_returns_400(http: Client) -> None:
    body = json.dumps("just a string")
    response = _post(http, body)
    assert response.status_code == 400


def test_empty_body_returns_400(http: Client) -> None:
    # Empty body has a valid signature but parses to None → not an object.
    response = _post(http, "")
    assert response.status_code == 400


# ─── method handling ───────────────────────────────────────────────────────


def test_get_returns_405(http: Client) -> None:
    response = http.get(WEBHOOK_URL)
    assert response.status_code == 405


def test_put_returns_405(http: Client) -> None:
    response = http.put(WEBHOOK_URL, data="{}", content_type="application/json")
    assert response.status_code == 405


# ─── tolerance configuration ───────────────────────────────────────────────


@override_settings(
    REROUT={
        "API_KEY": "rrk_test_key",
        "WEBHOOK_SECRET": TEST_WEBHOOK_SECRET,
        "SIGNATURE_TOLERANCE_SECONDS": 0,
    }
)
def test_tolerance_zero_accepts_old_timestamp(http: Client) -> None:
    body = json.dumps({"event": "link.clicked"})
    old_ts = int(time.time()) - 100_000
    response = _post(http, body, timestamp=old_ts)
    assert response.status_code == 200


# ─── misconfiguration ──────────────────────────────────────────────────────


@override_settings(REROUT={"API_KEY": "rrk_test_key"})
def test_missing_webhook_secret_raises(http: Client) -> None:
    from django.core.exceptions import ImproperlyConfigured

    body = json.dumps({"event": "link.clicked"})
    with pytest.raises(ImproperlyConfigured):
        _post(http, body)
