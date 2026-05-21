"""Error type raised by every Rerout SDK call."""

from __future__ import annotations

from typing import Any


class ReroutError(Exception):
    """Exception raised for any failed Rerout API call.

    The ``code`` field carries the stable string identifier returned by the
    Rerout API (for example ``bad_target_url``, ``rate_limited``,
    ``not_found``) so callers can branch on it without parsing the
    human-readable ``message``.

    For network or non-JSON failures the ``code`` is one of the synthetic
    client-side values: ``network_error``, ``timeout``,
    ``unexpected_response``, ``unauthorized``, ``forbidden``, ``not_found``,
    ``rate_limited``, ``server_error``, ``client_error``, ``missing_api_key``,
    ``bad_request``.

    Attributes:
        code: Stable error code from the API or synthesized client-side.
        status: HTTP status code, or ``0`` when the request never reached
            the server.
        message: Human-readable error message.
        details: Optional raw response body (parsed JSON or string), useful
            for debugging.
        path: Optional API path that produced the error.
        timestamp: Optional server-supplied ISO 8601 timestamp.
    """

    code: str
    status: int
    details: Any
    path: str | None
    timestamp: str | None

    def __init__(
        self,
        *,
        code: str,
        message: str,
        status: int,
        details: Any = None,
        path: str | None = None,
        timestamp: str | None = None,
    ) -> None:
        super().__init__(message)
        self.code = code
        self.status = status
        self.message = message
        self.details = details
        self.path = path
        self.timestamp = timestamp

    @property
    def is_server_error(self) -> bool:
        """True for HTTP 5xx responses (server-side issues)."""
        return 500 <= self.status < 600

    @property
    def is_rate_limited(self) -> bool:
        """True for HTTP 429 — caller should back off and retry."""
        return self.status == 429

    def __repr__(self) -> str:
        return (
            f"ReroutError(code={self.code!r}, status={self.status!r}, "
            f"message={self.message!r})"
        )
