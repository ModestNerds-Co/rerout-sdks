"""Tests for the webhook signature verification helper."""

from __future__ import annotations

import hashlib
import hmac

import pytest

from rerout import DEFAULT_SIGNATURE_TOLERANCE_SECONDS, verify_rerout_signature

SECRET = "whsec_test_secret"
BODY = '{"event":"link.clicked","code":"q4"}'
FIXED_NOW = 1_700_000_000


def sign(body: str, secret: str, timestamp: int) -> str:
    """Build a valid ``t=…,v1=…`` header for the given body/secret/timestamp."""
    mac = hmac.new(
        secret.encode("utf-8"),
        f"{timestamp}.{body}".encode(),
        hashlib.sha256,
    ).hexdigest()
    return f"t={timestamp},v1={mac}"


def at(now: int):  # type: ignore[no-untyped-def]
    """A fixed-clock callable for deterministic tolerance tests."""
    return lambda: now


# ─── Valid signatures ──────────────────────────────────────────────────────


def test_valid_fresh_signature() -> None:
    header = sign(BODY, SECRET, FIXED_NOW)
    assert verify_rerout_signature(BODY, header, SECRET, now=at(FIXED_NOW)) is True


def test_valid_with_real_clock() -> None:
    import time

    now = int(time.time())
    header = sign(BODY, SECRET, now)
    assert verify_rerout_signature(BODY, header, SECRET) is True


def test_valid_empty_body() -> None:
    header = sign("", SECRET, FIXED_NOW)
    assert verify_rerout_signature("", header, SECRET, now=at(FIXED_NOW)) is True


# ─── Rejection: wrong HMAC ─────────────────────────────────────────────────


def test_wrong_secret_rejected() -> None:
    header = sign(BODY, "whsec_other_secret", FIXED_NOW)
    assert verify_rerout_signature(BODY, header, SECRET, now=at(FIXED_NOW)) is False


def test_tampered_body_rejected() -> None:
    header = sign(BODY, SECRET, FIXED_NOW)
    tampered = BODY + " "
    assert verify_rerout_signature(tampered, header, SECRET, now=at(FIXED_NOW)) is False


def test_wrong_v1_value_rejected() -> None:
    header = f"t={FIXED_NOW},v1={'0' * 64}"
    assert verify_rerout_signature(BODY, header, SECRET, now=at(FIXED_NOW)) is False


# ─── Rejection: timestamp tolerance ────────────────────────────────────────


def test_expired_signature_rejected() -> None:
    header = sign(BODY, SECRET, FIXED_NOW)
    # 301s drift with the default 300s window.
    later = FIXED_NOW + DEFAULT_SIGNATURE_TOLERANCE_SECONDS + 1
    assert verify_rerout_signature(BODY, header, SECRET, now=at(later)) is False


def test_future_signature_rejected() -> None:
    header = sign(BODY, SECRET, FIXED_NOW)
    earlier = FIXED_NOW - DEFAULT_SIGNATURE_TOLERANCE_SECONDS - 1
    assert verify_rerout_signature(BODY, header, SECRET, now=at(earlier)) is False


def test_exactly_at_tolerance_boundary_accepted() -> None:
    header = sign(BODY, SECRET, FIXED_NOW)
    edge = FIXED_NOW + DEFAULT_SIGNATURE_TOLERANCE_SECONDS
    assert verify_rerout_signature(BODY, header, SECRET, now=at(edge)) is True


def test_exactly_at_tolerance_boundary_past_accepted() -> None:
    header = sign(BODY, SECRET, FIXED_NOW)
    edge = FIXED_NOW - DEFAULT_SIGNATURE_TOLERANCE_SECONDS
    assert verify_rerout_signature(BODY, header, SECRET, now=at(edge)) is True


def test_tolerance_zero_disables_timestamp_check() -> None:
    header = sign(BODY, SECRET, FIXED_NOW)
    way_later = FIXED_NOW + 10_000_000
    assert (
        verify_rerout_signature(
            BODY, header, SECRET, tolerance_seconds=0, now=at(way_later)
        )
        is True
    )


def test_custom_tolerance_window() -> None:
    header = sign(BODY, SECRET, FIXED_NOW)
    assert (
        verify_rerout_signature(
            BODY, header, SECRET, tolerance_seconds=60, now=at(FIXED_NOW + 30)
        )
        is True
    )
    assert (
        verify_rerout_signature(
            BODY, header, SECRET, tolerance_seconds=60, now=at(FIXED_NOW + 90)
        )
        is False
    )


# ─── Rejection: malformed headers ──────────────────────────────────────────


@pytest.mark.parametrize(
    "header",
    [
        "",
        "garbage",
        "t=,v1=abc",
        f"v1={'a' * 64}",  # missing t
        f"t={FIXED_NOW}",  # missing v1
        f"t=notanumber,v1={'a' * 64}",  # non-numeric t
        f"t={FIXED_NOW},v1=zzzz",  # non-hex v1
        f"t={FIXED_NOW},v1=abc",  # odd-length v1
        f"t=-5,v1={'a' * 64}",  # non-positive t
        f"t=0,v1={'a' * 64}",  # zero t
    ],
)
def test_malformed_headers_rejected(header: str) -> None:
    assert verify_rerout_signature(BODY, header, SECRET, now=at(FIXED_NOW)) is False


# ─── Casing variations ─────────────────────────────────────────────────────


def test_uppercase_keys_accepted() -> None:
    mac = hmac.new(
        SECRET.encode("utf-8"),
        f"{FIXED_NOW}.{BODY}".encode(),
        hashlib.sha256,
    ).hexdigest()
    header = f"T={FIXED_NOW},V1={mac}"
    assert verify_rerout_signature(BODY, header, SECRET, now=at(FIXED_NOW)) is True


def test_mixed_case_keys_accepted() -> None:
    mac = hmac.new(
        SECRET.encode("utf-8"),
        f"{FIXED_NOW}.{BODY}".encode(),
        hashlib.sha256,
    ).hexdigest()
    header = f"T={FIXED_NOW},v1={mac}"
    assert verify_rerout_signature(BODY, header, SECRET, now=at(FIXED_NOW)) is True


def test_uppercase_hex_v1_accepted() -> None:
    mac = hmac.new(
        SECRET.encode("utf-8"),
        f"{FIXED_NOW}.{BODY}".encode(),
        hashlib.sha256,
    ).hexdigest()
    header = f"t={FIXED_NOW},v1={mac.upper()}"
    assert verify_rerout_signature(BODY, header, SECRET, now=at(FIXED_NOW)) is True


def test_whitespace_around_segments_tolerated() -> None:
    mac = hmac.new(
        SECRET.encode("utf-8"),
        f"{FIXED_NOW}.{BODY}".encode(),
        hashlib.sha256,
    ).hexdigest()
    header = f" t = {FIXED_NOW} , v1 = {mac} "
    assert verify_rerout_signature(BODY, header, SECRET, now=at(FIXED_NOW)) is True


# ─── Rejection: empty secret / header ──────────────────────────────────────


def test_empty_secret_rejected() -> None:
    header = sign(BODY, SECRET, FIXED_NOW)
    assert verify_rerout_signature(BODY, header, "", now=at(FIXED_NOW)) is False


def test_empty_header_rejected() -> None:
    assert verify_rerout_signature(BODY, "", SECRET, now=at(FIXED_NOW)) is False


def test_default_tolerance_constant() -> None:
    assert DEFAULT_SIGNATURE_TOLERANCE_SECONDS == 300
