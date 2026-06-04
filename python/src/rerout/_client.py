"""Synchronous HTTP client for the Rerout API.

Built on :mod:`httpx` so callers can inject a custom :class:`httpx.Client` for
mounting transports, proxies, custom timeouts, and so on. The default client
is a fresh :class:`httpx.Client` per :class:`Rerout` instance.
"""

from __future__ import annotations

import json
from collections.abc import Iterable
from typing import Any
from urllib.parse import quote, urlencode

import httpx

from ._errors import ReroutError
from ._models import (
    BatchCreateLinksResult,
    CreatedWebhook,
    CreateLinkInput,
    CreateWebhookInput,
    Link,
    LinkStats,
    ListLinksResult,
    ListWebhooksResult,
    ProjectInfo,
    ProjectStats,
    QrOptions,
    RecordedConversion,
    UpdateLinkInput,
)

DEFAULT_BASE_URL = "https://api.rerout.co"
"""Default production API URL. Override via ``base_url`` for staging or
self-hosted setups."""


class Rerout:
    """Official Python client for the Rerout API.

    Example:
        >>> from rerout import Rerout, CreateLinkInput
        >>> rerout = Rerout(api_key="rrk_...")
        >>> link = rerout.links.create(
        ...     CreateLinkInput(target_url="https://example.com/q4-sale")
        ... )
        >>> print(link.short_url)
    """

    links: Links
    project: Project
    qr: Qr
    webhooks: Webhooks
    conversions: Conversions

    def __init__(
        self,
        api_key: str,
        *,
        base_url: str = DEFAULT_BASE_URL,
        client: httpx.Client | None = None,
        timeout: float = 30.0,
    ) -> None:
        """Create a new client.

        Args:
            api_key: Project API key (``rrk_…``). Required.
            base_url: Override the API base URL. Defaults to
                ``https://api.rerout.co``. Trailing slashes are trimmed.
            client: Inject a pre-configured :class:`httpx.Client`. Useful
                for transports, proxies, custom retries, or tests. The
                injected client's ``timeout`` and ``base_url`` are left
                untouched.
            timeout: Request timeout in seconds. Applied only when a
                fresh client is constructed — ignored if ``client`` is
                provided. Defaults to 30 seconds.

        Raises:
            ReroutError: If ``api_key`` is empty or not a string.
        """
        if not isinstance(api_key, str) or not api_key.strip():
            raise ReroutError(
                code="missing_api_key",
                message="A project API key is required to construct Rerout.",
                status=0,
            )

        self._api_key = api_key
        self._base_url = base_url.rstrip("/") if base_url else DEFAULT_BASE_URL
        self._owns_client = client is None
        self._client = client if client is not None else httpx.Client(timeout=timeout)

        self.links = Links(self)
        self.project = Project(self)
        self.qr = Qr(self)
        self.webhooks = Webhooks(self)
        self.conversions = Conversions(self)

    @property
    def base_url(self) -> str:
        """The resolved API base URL with trailing slashes stripped."""
        return self._base_url

    def close(self) -> None:
        """Close the underlying HTTP client if this instance created it.

        Injected clients are not closed — the caller owns their lifecycle.
        """
        if self._owns_client:
            self._client.close()

    def __enter__(self) -> Rerout:
        return self

    def __exit__(self, *_: object) -> None:
        self.close()

    # ─── Internal transport ────────────────────────────────────────────

    def _request(
        self,
        method: str,
        path: str,
        *,
        body: dict[str, Any] | None = None,
        query: dict[str, Any] | None = None,
        accept: str = "application/json",
        parse_json: bool = True,
    ) -> Any:
        """Send a request and return the parsed JSON body (or text).

        Args:
            method: HTTP verb.
            path: Path beginning with ``/``.
            body: JSON-serialisable body. ``None`` means no body.
            query: Query parameters. ``None``/``None``-valued entries
                are dropped.
            accept: Value for the ``Accept`` header. Defaults to
                ``application/json``.
            parse_json: When ``True``, the success body is parsed as JSON
                and returned as a Python object. When ``False`` the raw
                text is returned (used by :meth:`Qr.svg`).

        Raises:
            ReroutError: For any non-2xx response, network failure,
                timeout, or non-JSON success body when ``parse_json`` is
                ``True``.
        """
        url = self._base_url + path

        params: dict[str, Any] | None = None
        if query:
            params = {k: v for k, v in query.items() if v is not None}
            if not params:
                params = None

        headers = {
            "authorization": f"Bearer {self._api_key}",
            "accept": accept,
        }
        request_kwargs: dict[str, Any] = {
            "method": method,
            "url": url,
            "headers": headers,
            "params": params,
        }
        if body is not None:
            headers["content-type"] = "application/json"
            request_kwargs["content"] = json.dumps(body).encode("utf-8")

        try:
            response = self._client.request(**request_kwargs)
        except httpx.TimeoutException as exc:
            raise ReroutError(
                code="timeout",
                message=str(exc) or "Request to Rerout timed out.",
                status=0,
                details=str(exc),
                path=path,
            ) from exc
        except httpx.HTTPError as exc:
            raise ReroutError(
                code="network_error",
                message=str(exc) or "Request to Rerout failed before the server replied.",
                status=0,
                details=str(exc),
                path=path,
            ) from exc

        text = response.text
        status = response.status_code
        if status < 200 or status >= 300:
            raise _error_from_response(status, text, path)

        if not parse_json:
            return text

        if len(text) == 0:
            return None
        try:
            return json.loads(text)
        except ValueError as exc:
            raise ReroutError(
                code="unexpected_response",
                message="Rerout returned a non-JSON success body.",
                status=status,
                details={"body": text, "error": str(exc)},
                path=path,
            ) from exc


def _error_from_response(status: int, body: str, path: str) -> ReroutError:
    code = _synth_code_for_status(status)
    if not body:
        return ReroutError(
            code=code,
            message=f"Rerout returned HTTP {status} with no body.",
            status=status,
            path=path,
        )
    try:
        parsed = json.loads(body)
    except ValueError:
        return ReroutError(
            code=code,
            message=f"Rerout returned HTTP {status} (non-JSON body).",
            status=status,
            details={"body": body},
            path=path,
        )
    if not isinstance(parsed, dict):
        return ReroutError(
            code=code,
            message=f"Rerout returned HTTP {status} (unexpected body shape).",
            status=status,
            details=parsed,
            path=path,
        )
    server_code = parsed.get("code")
    server_message = parsed.get("message")
    server_timestamp = parsed.get("timestamp")
    return ReroutError(
        code=str(server_code) if isinstance(server_code, str) else code,
        message=(
            str(server_message)
            if isinstance(server_message, str)
            else f"Rerout returned HTTP {status}."
        ),
        status=status,
        details=parsed,
        path=path,
        timestamp=(str(server_timestamp) if isinstance(server_timestamp, str) else None),
    )


def _synth_code_for_status(status: int) -> str:
    if status == 401:
        return "unauthorized"
    if status == 403:
        return "forbidden"
    if status == 404:
        return "not_found"
    if status == 429:
        return "rate_limited"
    if 500 <= status < 600:
        return "server_error"
    return "client_error"


# ─── Namespaces ────────────────────────────────────────────────────────


class Links:
    """Link operations: create, list, get, update, delete, stats."""

    def __init__(self, client: Rerout) -> None:
        self._client = client

    def create(self, input: CreateLinkInput) -> Link:
        """Create a new short link."""
        data = self._client._request(
            "POST",
            "/v1/links",
            body=input.to_payload(),
        )
        return Link.from_dict(_expect_dict(data))

    def create_batch(
        self,
        inputs: Iterable[CreateLinkInput | dict[str, Any]],
    ) -> BatchCreateLinksResult:
        """Create many links in one call via ``POST /v1/links/batch``.

        Each item may be a :class:`CreateLinkInput` or a plain dict. Only the
        batch-supported fields (``target_url``, ``code``, ``expires_at``,
        ``domain_hostname``) are forwarded — the richer Smart Link fields are
        not accepted by the batch endpoint.

        Returns a :class:`BatchCreateLinksResult` carrying per-item outcomes;
        a failed item does not raise — inspect ``result.results[i].ok``.

        Raises:
            ReroutError: With ``code='bad_request'`` if ``inputs`` is empty.
        """
        links = [_batch_link_payload(item) for item in inputs]
        if not links:
            raise ReroutError(
                code="bad_request",
                message="create_batch requires at least one link.",
                status=0,
                path="/v1/links/batch",
            )
        data = self._client._request(
            "POST",
            "/v1/links/batch",
            body={"links": links},
        )
        return BatchCreateLinksResult.from_dict(_expect_dict(data))

    def list(
        self,
        *,
        cursor: int | None = None,
        limit: int | None = None,
    ) -> ListLinksResult:
        """Paginated list of links in the project."""
        data = self._client._request(
            "GET",
            "/v1/links",
            query={"cursor": cursor, "limit": limit},
        )
        return ListLinksResult.from_dict(_expect_dict(data))

    def get(self, code: str) -> Link:
        """Get a single link by code."""
        data = self._client._request(
            "GET",
            f"/v1/links/{_encode_code(code)}",
        )
        return Link.from_dict(_expect_dict(data))

    def update(self, code: str, input: UpdateLinkInput) -> Link:
        """Patch a link. Only fields set on ``input`` are sent.

        Raises:
            ReroutError: With ``code='bad_request'`` if ``input`` has no
                fields set — sending an empty PATCH body would be wasted.
        """
        if input.is_empty:
            raise ReroutError(
                code="bad_request",
                message="UpdateLinkInput has no fields to send.",
                status=0,
                path=f"/v1/links/{code}",
            )
        data = self._client._request(
            "PATCH",
            f"/v1/links/{_encode_code(code)}",
            body=input.to_payload(),
        )
        return Link.from_dict(_expect_dict(data))

    def delete(self, code: str) -> bool:
        """Soft-delete a link. Returns ``True`` on success.

        The short URL stops redirecting and is gone from lists.
        """
        data = self._client._request(
            "DELETE",
            f"/v1/links/{_encode_code(code)}",
        )
        if isinstance(data, dict):
            return bool(data.get("deleted", True))
        return True

    def stats(self, code: str, days: int = 30) -> LinkStats:
        """Per-link click stats. Defaults to 30 days."""
        data = self._client._request(
            "GET",
            f"/v1/links/{_encode_code(code)}/stats",
            query={"days": days},
        )
        return LinkStats.from_dict(_expect_dict(data))


class Project:
    """Project-level operations: aggregate stats + identity."""

    def __init__(self, client: Rerout) -> None:
        self._client = client

    def stats(self, days: int = 30) -> ProjectStats:
        """Aggregate stats across every link in the project."""
        data = self._client._request(
            "GET",
            "/v1/projects/me/stats",
            query={"days": days},
        )
        return ProjectStats.from_dict(_expect_dict(data))

    def me(self) -> ProjectInfo:
        """Info about the project that owns the current API key."""
        data = self._client._request("GET", "/v1/projects/me")
        return ProjectInfo.from_dict(_expect_dict(data))


class Webhooks:
    """Webhook endpoint management: create, list, delete.

    Operates on the project that owns the API key — the project is resolved
    from the key, so no project id appears in the path.
    """

    def __init__(self, client: Rerout) -> None:
        self._client = client

    def create(self, input: CreateWebhookInput) -> CreatedWebhook:
        """Create a webhook endpoint for the project that owns the API key.

        The returned ``signing_secret`` is shown once — persist it to verify
        deliveries with :func:`verify_rerout_signature`.
        """
        data = self._client._request(
            "POST",
            "/v1/projects/me/webhooks",
            body=input.to_payload(),
        )
        return CreatedWebhook.from_dict(_expect_dict(data))

    def list(self) -> ListWebhooksResult:
        """List webhook endpoints and the event types the server can deliver."""
        data = self._client._request(
            "GET",
            "/v1/projects/me/webhooks",
        )
        return ListWebhooksResult.from_dict(_expect_dict(data))

    def delete(self, endpoint_id: str) -> bool:
        """Soft-delete an endpoint. Returns ``True`` on success.

        Idempotent — deleting an already-gone endpoint still succeeds.
        """
        data = self._client._request(
            "DELETE",
            f"/v1/projects/me/webhooks/{_encode_code(endpoint_id)}",
        )
        if isinstance(data, dict):
            return bool(data.get("deleted", True))
        return True


class Conversions:
    """Conversion tracking: record a conversion against a recorded click."""

    def __init__(self, client: Rerout) -> None:
        self._client = client

    def record(
        self,
        click_id: str,
        event_name: str,
        *,
        value_cents: int | None = None,
        currency: str | None = None,
    ) -> RecordedConversion:
        """Record a conversion for a click via ``POST /v1/conversions``.

        Args:
            click_id: The id of the click being converted (from the redirect
                tracking pixel / postback).
            event_name: Name of the conversion event (e.g. ``"purchase"``).
            value_cents: Optional monetary value of the conversion, in the
                smallest currency unit (cents). Omitted when ``None``.
            currency: Optional ISO-4217 currency code (e.g. ``"USD"``).
                Omitted when ``None``.

        Returns:
            A :class:`RecordedConversion` with ``recorded`` and ``duplicate``
            flags. The call is idempotent — a repeat for the same click +
            event returns ``duplicate=True``.
        """
        body: dict[str, Any] = {
            "click_id": click_id,
            "event_name": event_name,
        }
        if value_cents is not None:
            body["value_cents"] = value_cents
        if currency is not None:
            body["currency"] = currency
        data = self._client._request("POST", "/v1/conversions", body=body)
        return RecordedConversion.from_dict(_expect_dict(data))


class Qr:
    """QR helpers — pure URL builder + signed SVG fetch."""

    def __init__(self, client: Rerout) -> None:
        self._client = client

    def url(self, code: str, options: QrOptions | None = None) -> str:
        """Build the URL the API serves the QR SVG from.

        Pure — does not call the API. Authentication is the caller's
        responsibility — the endpoint is API-key authenticated, so any
        ``<img>`` tag will need a way to send the bearer token (typically
        via a server-side proxy).
        """
        base = self._client.base_url.rstrip("/")
        path = f"{base}/v1/links/{_encode_code(code)}/qr"
        params = options.to_query_params() if options is not None else {}
        if not params:
            return path
        return f"{path}?{urlencode(params)}"

    def svg(self, code: str, options: QrOptions | None = None) -> str:
        """Fetch the QR as an SVG string.

        Hits the same endpoint as :meth:`url` but attaches the bearer
        token and returns the rendered body.
        """
        params = options.to_query_params() if options is not None else None
        result = self._client._request(
            "GET",
            f"/v1/links/{_encode_code(code)}/qr",
            query=dict(params) if params else None,
            accept="image/svg+xml,text/html",
            parse_json=False,
        )
        return str(result)


def _encode_code(code: str) -> str:
    """Percent-encode a path-segment code.

    Uses an empty ``safe`` set so ``/``, ``+``, spaces, and unicode are all
    escaped — mirroring ``encodeURIComponent`` in JS.
    """
    return quote(code, safe="")


def _batch_link_payload(item: CreateLinkInput | dict[str, Any]) -> dict[str, Any]:
    """Reduce a create-link input to the fields the batch endpoint accepts.

    Only ``target_url`` (required), ``code``, ``expires_at``, and
    ``domain_hostname`` are forwarded.
    """
    if isinstance(item, CreateLinkInput):
        source = item.to_payload()
    elif isinstance(item, dict):
        source = item
    else:
        raise ReroutError(
            code="bad_request",
            message="Batch link items must be CreateLinkInput or dict.",
            status=0,
            path="/v1/links/batch",
        )
    if "target_url" not in source:
        raise ReroutError(
            code="bad_request",
            message="Each batch link requires a target_url.",
            status=0,
            path="/v1/links/batch",
        )
    payload: dict[str, Any] = {"target_url": source["target_url"]}
    for key in ("code", "expires_at", "domain_hostname"):
        if source.get(key) is not None:
            payload[key] = source[key]
    return payload


def _expect_dict(data: Any) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ReroutError(
            code="unexpected_response",
            message="Expected JSON object from Rerout but received a different shape.",
            status=0,
            details=data,
        )
    return data
