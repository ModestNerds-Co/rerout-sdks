"""Constructor + request-transport tests for the Rerout client."""

from __future__ import annotations

import httpx
import pytest
import respx

from conftest import TEST_API_KEY, sample_link
from rerout import DEFAULT_BASE_URL, CreateLinkInput, Rerout, ReroutError

# ─── Constructor ───────────────────────────────────────────────────────────


def test_constructor_requires_api_key() -> None:
    with pytest.raises(ReroutError) as exc:
        Rerout(api_key="")
    assert exc.value.code == "missing_api_key"
    assert exc.value.status == 0


def test_constructor_rejects_whitespace_api_key() -> None:
    with pytest.raises(ReroutError) as exc:
        Rerout(api_key="   ")
    assert exc.value.code == "missing_api_key"


def test_constructor_rejects_non_string_api_key() -> None:
    with pytest.raises(ReroutError) as exc:
        Rerout(api_key=None)  # type: ignore[arg-type]
    assert exc.value.code == "missing_api_key"


def test_default_base_url() -> None:
    rerout = Rerout(api_key=TEST_API_KEY)
    assert rerout.base_url == DEFAULT_BASE_URL
    rerout.close()


def test_base_url_trailing_slash_trimmed() -> None:
    rerout = Rerout(api_key=TEST_API_KEY, base_url="https://api.rerout.co/")
    assert rerout.base_url == "https://api.rerout.co"
    rerout.close()


def test_base_url_multiple_trailing_slashes_trimmed() -> None:
    rerout = Rerout(api_key=TEST_API_KEY, base_url="https://api.rerout.co///")
    assert rerout.base_url == "https://api.rerout.co"
    rerout.close()


def test_namespaces_present() -> None:
    rerout = Rerout(api_key=TEST_API_KEY)
    assert rerout.links is not None
    assert rerout.project is not None
    assert rerout.qr is not None
    rerout.close()


def test_context_manager_closes_owned_client() -> None:
    with Rerout(api_key=TEST_API_KEY) as rerout:
        inner = rerout._client
    assert inner.is_closed


def test_injected_client_not_closed_on_close() -> None:
    custom = httpx.Client()
    rerout = Rerout(api_key=TEST_API_KEY, client=custom)
    rerout.close()
    assert not custom.is_closed
    custom.close()


# ─── Request transport ─────────────────────────────────────────────────────


def test_bearer_auth_header_sent(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.get("/v1/projects/me").mock(
        return_value=httpx.Response(200, json={"id": "p", "name": "N", "slug": "s"})
    )
    client.project.me()
    assert route.calls.last.request.headers["authorization"] == f"Bearer {TEST_API_KEY}"


def test_accept_header_sent(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.get("/v1/projects/me").mock(
        return_value=httpx.Response(200, json={"id": "p", "name": "N", "slug": "s"})
    )
    client.project.me()
    assert route.calls.last.request.headers["accept"] == "application/json"


def test_content_type_present_only_with_body(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    get_route = mock_api.get("/v1/projects/me").mock(
        return_value=httpx.Response(200, json={"id": "p", "name": "N", "slug": "s"})
    )
    post_route = mock_api.post("/v1/links").mock(
        return_value=httpx.Response(200, json=sample_link())
    )

    client.project.me()
    client.links.create(CreateLinkInput(target_url="https://example.com"))

    assert "content-type" not in get_route.calls.last.request.headers
    assert post_route.calls.last.request.headers["content-type"] == "application/json"


def test_json_body_serialized(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.post("/v1/links").mock(
        return_value=httpx.Response(200, json=sample_link())
    )
    client.links.create(
        CreateLinkInput(target_url="https://example.com/x", code="abc")
    )
    body = route.calls.last.request.content
    assert b'"target_url"' in body
    assert b'"https://example.com/x"' in body
    assert b'"code"' in body


def test_query_params_for_days(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.get("/v1/projects/me/stats").mock(
        return_value=httpx.Response(
            200,
            json={
                "days": 7,
                "total_clicks": 0,
                "qr_scans": 0,
                "daily": [],
                "countries": [],
                "referrers": [],
                "devices": [],
                "browsers": [],
                "top_codes": [],
            },
        )
    )
    client.project.stats(days=7)
    assert route.calls.last.request.url.params["days"] == "7"


def test_query_params_for_cursor_and_limit(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.get("/v1/links").mock(
        return_value=httpx.Response(200, json={"links": [], "next_cursor": None})
    )
    client.links.list(cursor=40, limit=10)
    params = route.calls.last.request.url.params
    assert params["cursor"] == "40"
    assert params["limit"] == "10"


def test_query_params_omitted_when_none(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.get("/v1/links").mock(
        return_value=httpx.Response(200, json={"links": [], "next_cursor": None})
    )
    client.links.list()
    assert "cursor" not in route.calls.last.request.url.params
    assert "limit" not in route.calls.last.request.url.params


# ─── Error parsing ─────────────────────────────────────────────────────────


def test_error_server_code_and_message_preserved(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.post("/v1/links").mock(
        return_value=httpx.Response(
            400,
            json={
                "code": "bad_target_url",
                "message": "target_url must use https.",
                "timestamp": "2026-05-21T00:00:00Z",
            },
        )
    )
    with pytest.raises(ReroutError) as exc:
        client.links.create(CreateLinkInput(target_url="http://insecure"))
    assert exc.value.code == "bad_target_url"
    assert exc.value.message == "target_url must use https."
    assert exc.value.status == 400
    assert exc.value.timestamp == "2026-05-21T00:00:00Z"
    assert exc.value.path == "/v1/links"
    assert isinstance(exc.value.details, dict)


@pytest.mark.parametrize(
    ("status", "code"),
    [
        (401, "unauthorized"),
        (403, "forbidden"),
        (404, "not_found"),
        (429, "rate_limited"),
        (500, "server_error"),
        (503, "server_error"),
        (418, "client_error"),
    ],
)
def test_synthetic_codes_for_status_with_no_body(
    mock_api: respx.MockRouter, client: Rerout, status: int, code: str
) -> None:
    mock_api.get("/v1/projects/me").mock(
        return_value=httpx.Response(status, content=b"")
    )
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.code == code
    assert exc.value.status == status


def test_synthetic_code_for_non_json_error_body(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.get("/v1/projects/me").mock(
        return_value=httpx.Response(500, content=b"<html>oops</html>")
    )
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.code == "server_error"
    assert exc.value.details == {"body": "<html>oops</html>"}


def test_error_non_dict_json_body(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.get("/v1/projects/me").mock(
        return_value=httpx.Response(400, json=["unexpected", "list"])
    )
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.code == "client_error"
    assert exc.value.details == ["unexpected", "list"]


def test_is_rate_limited_flag(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/projects/me").mock(return_value=httpx.Response(429))
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.is_rate_limited is True
    assert exc.value.is_server_error is False


def test_is_server_error_flag(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/projects/me").mock(return_value=httpx.Response(502))
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.is_server_error is True
    assert exc.value.is_rate_limited is False


# ─── Network failures ──────────────────────────────────────────────────────


def test_network_failure_maps_to_network_error(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.get("/v1/projects/me").mock(
        side_effect=httpx.ConnectError("connection refused")
    )
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.code == "network_error"
    assert exc.value.status == 0
    assert exc.value.path == "/v1/projects/me"


def test_timeout_maps_to_timeout(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.get("/v1/projects/me").mock(
        side_effect=httpx.ReadTimeout("timed out")
    )
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.code == "timeout"
    assert exc.value.status == 0


# ─── Unexpected success bodies ─────────────────────────────────────────────


def test_unexpected_response_on_2xx_non_json(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.get("/v1/projects/me").mock(
        return_value=httpx.Response(200, content=b"not json at all")
    )
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.code == "unexpected_response"
    assert exc.value.status == 200


def test_unexpected_response_on_2xx_non_object(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.get("/v1/projects/me").mock(
        return_value=httpx.Response(200, json="just a string")
    )
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.code == "unexpected_response"


def test_empty_2xx_body_returns_none_path(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    # DELETE with an empty body should still succeed (treated as deleted).
    mock_api.delete("/v1/links/q4").mock(
        return_value=httpx.Response(200, content=b"")
    )
    assert client.links.delete("q4") is True


def test_error_with_no_body_has_message(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.get("/v1/projects/me").mock(return_value=httpx.Response(404))
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert "404" in exc.value.message


def test_repr_includes_code_and_status() -> None:
    err = ReroutError(code="not_found", message="missing", status=404)
    text = repr(err)
    assert "not_found" in text
    assert "404" in text
