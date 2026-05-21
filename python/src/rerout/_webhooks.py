"""Webhook signature verification helper.

Rerout signs every webhook delivery as ``t={unix_seconds},v1={hex_hmac_sha256}``
where the HMAC is computed over ``"{timestamp}.{raw_body}"`` with the endpoint
signing secret as the key. Constant-time comparison + a 5-minute timestamp
tolerance window protect against replay attacks.
"""

from __future__ import annotations

import hashlib
import hmac
import time
from collections.abc import Callable
from dataclasses import dataclass

DEFAULT_SIGNATURE_TOLERANCE_SECONDS = 300
"""Default window (in seconds) between the ``t=`` timestamp and the server's
current time. Set to 0 to disable the timestamp staleness check."""


@dataclass(frozen=True, slots=True)
class _ParsedSignature:
    timestamp: int
    v1: str


def verify_rerout_signature(
    raw_body: str,
    signature_header: str,
    secret: str,
    *,
    tolerance_seconds: int = DEFAULT_SIGNATURE_TOLERANCE_SECONDS,
    now: Callable[[], int] | None = None,
) -> bool:
    """Return ``True`` if ``signature_header`` is a valid Rerout signature.

    Args:
        raw_body: Raw, unmodified request body as a string.
        signature_header: Value of the ``X-Rerout-Signature`` header.
        secret: Endpoint signing secret (``whsec_…``) from the dashboard.
        tolerance_seconds: Allowed drift between the ``t=`` timestamp and
            the current time. Defaults to 300 seconds. Pass ``0`` to
            disable the staleness check.
        now: Injectable clock returning unix seconds. Defaults to
            :func:`time.time`. Useful for deterministic tests.

    Returns:
        ``True`` only when the header parses cleanly, the timestamp is
        within ``tolerance_seconds`` of ``now()`` (or the check is
        disabled), and the computed HMAC matches the supplied ``v1`` in
        constant time. ``False`` in every other case — including empty
        header, empty secret, malformed header, missing ``t`` or ``v1``,
        non-numeric or non-positive ``t``, non-hex or odd-length ``v1``,
        timestamp outside tolerance, or HMAC mismatch.
    """
    if not signature_header or not secret:
        return False

    parsed = _parse_header(signature_header)
    if parsed is None:
        return False

    if tolerance_seconds > 0:
        current = now() if now is not None else int(time.time())
        if abs(current - parsed.timestamp) > tolerance_seconds:
            return False

    expected_hex = hmac.new(
        secret.encode("utf-8"),
        f"{parsed.timestamp}.{raw_body}".encode(),
        hashlib.sha256,
    ).hexdigest()

    try:
        expected_bytes = bytes.fromhex(expected_hex)
        actual_bytes = bytes.fromhex(parsed.v1)
    except ValueError:
        return False

    if len(expected_bytes) != len(actual_bytes):
        return False

    return hmac.compare_digest(expected_bytes, actual_bytes)


def _parse_header(header: str) -> _ParsedSignature | None:
    timestamp: int | None = None
    v1: str | None = None

    for segment in header.split(","):
        eq = segment.find("=")
        if eq <= 0:
            continue
        key = segment[:eq].strip().lower()
        value = segment[eq + 1 :].strip()
        if key == "t":
            try:
                parsed_ts = int(value)
            except ValueError:
                continue
            if parsed_ts > 0:
                timestamp = parsed_ts
        elif key == "v1":
            if value and _is_hex(value) and len(value) % 2 == 0:
                v1 = value

    if timestamp is None or v1 is None:
        return None
    return _ParsedSignature(timestamp=timestamp, v1=v1)


def _is_hex(value: str) -> bool:
    return all(c in "0123456789abcdefABCDEF" for c in value)
