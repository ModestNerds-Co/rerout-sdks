"""Tests for the QR URL builder and signed SVG fetch."""

from __future__ import annotations

from urllib.parse import parse_qs, urlsplit

import httpx
import respx

from conftest import TEST_API_KEY, TEST_BASE_URL
from rerout import QrOptions, Rerout

# ─── qr.url — pure builder ─────────────────────────────────────────────────


def test_url_bare_no_options(client: Rerout) -> None:
    assert client.qr.url("q4") == f"{TEST_BASE_URL}/v1/links/q4/qr"


def test_url_bare_with_empty_options(client: Rerout) -> None:
    assert client.qr.url("q4", QrOptions()) == f"{TEST_BASE_URL}/v1/links/q4/qr"


def test_url_emits_every_option(client: Rerout) -> None:
    url = client.qr.url(
        "q4",
        QrOptions(size=12, margin=2, ecc="H", domain="go.brand.com", refresh="v2"),
    )
    query = parse_qs(urlsplit(url).query)
    assert query["size"] == ["12"]
    assert query["margin"] == ["2"]
    assert query["ecc"] == ["H"]
    assert query["domain"] == ["go.brand.com"]
    assert query["refresh"] == ["v2"]


def test_url_refresh_true_becomes_one(client: Rerout) -> None:
    url = client.qr.url("q4", QrOptions(refresh=True))
    assert parse_qs(urlsplit(url).query)["refresh"] == ["1"]


def test_url_refresh_false_becomes_false_string(client: Rerout) -> None:
    url = client.qr.url("q4", QrOptions(refresh=False))
    # ``refresh=False`` is still "set" (not None), so it serialises verbatim.
    assert parse_qs(urlsplit(url).query)["refresh"] == ["False"]


def test_url_refresh_string_passes_through(client: Rerout) -> None:
    url = client.qr.url("q4", QrOptions(refresh="v2"))
    assert parse_qs(urlsplit(url).query)["refresh"] == ["v2"]


def test_url_honours_custom_base_url() -> None:
    rerout = Rerout(api_key=TEST_API_KEY, base_url="https://api.staging.rerout.co")
    url = rerout.qr.url("q4")
    assert url == "https://api.staging.rerout.co/v1/links/q4/qr"
    rerout.close()


def test_url_default_base_url() -> None:
    rerout = Rerout(api_key=TEST_API_KEY)
    assert rerout.qr.url("q4") == "https://api.rerout.co/v1/links/q4/qr"
    rerout.close()


def test_url_encodes_code(client: Rerout) -> None:
    assert client.qr.url("go/promo").endswith("/v1/links/go%2Fpromo/qr")


def test_url_partial_options(client: Rerout) -> None:
    url = client.qr.url("q4", QrOptions(size=20))
    query = parse_qs(urlsplit(url).query)
    assert query == {"size": ["20"]}


# ─── qr.svg — signed fetch ─────────────────────────────────────────────────


SVG_BODY = '<svg xmlns="http://www.w3.org/2000/svg"></svg>'


def test_svg_returns_body(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links/q4/qr").mock(
        return_value=httpx.Response(
            200, content=SVG_BODY.encode(), headers={"content-type": "image/svg+xml"}
        )
    )
    assert client.qr.svg("q4") == SVG_BODY


def test_svg_sends_bearer_token(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.get("/v1/links/q4/qr").mock(
        return_value=httpx.Response(200, content=SVG_BODY.encode())
    )
    client.qr.svg("q4")
    assert route.calls.last.request.headers["authorization"] == f"Bearer {TEST_API_KEY}"


def test_svg_sends_options_as_query(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.get("/v1/links/q4/qr").mock(
        return_value=httpx.Response(200, content=SVG_BODY.encode())
    )
    client.qr.svg("q4", QrOptions(size=16, ecc="Q", refresh=True))
    params = route.calls.last.request.url.params
    assert params["size"] == "16"
    assert params["ecc"] == "Q"
    assert params["refresh"] == "1"


def test_svg_accept_header(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.get("/v1/links/q4/qr").mock(
        return_value=httpx.Response(200, content=SVG_BODY.encode())
    )
    client.qr.svg("q4")
    assert "image/svg+xml" in route.calls.last.request.headers["accept"]


def test_svg_encodes_code(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.get("/v1/links/go%2Fpromo/qr").mock(
        return_value=httpx.Response(200, content=SVG_BODY.encode())
    )
    client.qr.svg("go/promo")
    assert route.called


def test_svg_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    import pytest

    from rerout import ReroutError

    mock_api.get("/v1/links/missing/qr").mock(
        return_value=httpx.Response(
            404, json={"code": "not_found", "message": "no such link"}
        )
    )
    with pytest.raises(ReroutError) as exc:
        client.qr.svg("missing")
    assert exc.value.code == "not_found"
