"""Tests for links.create_batch (POST /v1/links/batch).

Mirrors the links tests: a respx-mocked HTTP layer, no network access.
"""

from __future__ import annotations

import json

import httpx
import pytest
import respx

from rerout import (
    BatchCreateLinksResult,
    BatchLinkResult,
    CreateLinkInput,
    Rerout,
    ReroutError,
)


def _batch_response() -> dict[str, object]:
    return {
        "created": 2,
        "total": 3,
        "results": [
            {"index": 0, "ok": True, "code": "abc"},
            {"index": 1, "ok": True, "code": "def"},
            {"index": 2, "ok": False, "error": "bad_target_url"},
        ],
    }


def test_create_batch_posts_links_array(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.post("/v1/links/batch").mock(
        return_value=httpx.Response(200, json=_batch_response())
    )
    result = client.links.create_batch(
        [
            CreateLinkInput(target_url="https://example.com/1"),
            CreateLinkInput(
                target_url="https://example.com/2",
                code="def",
                expires_at=1_800_000_000,
                domain_hostname="go.brand.com",
            ),
            CreateLinkInput(target_url="bad"),
        ]
    )
    assert isinstance(result, BatchCreateLinksResult)
    assert result.created == 2
    assert result.total == 3
    assert len(result.results) == 3

    first = result.results[0]
    assert isinstance(first, BatchLinkResult)
    assert first.index == 0
    assert first.ok is True
    assert first.code == "abc"

    failed = result.results[2]
    assert failed.ok is False
    assert failed.error == "bad_target_url"
    assert failed.code is None

    body = json.loads(route.calls.last.request.content)
    assert route.calls.last.request.url.path == "/v1/links/batch"
    assert body == {
        "links": [
            {"target_url": "https://example.com/1"},
            {
                "target_url": "https://example.com/2",
                "code": "def",
                "expires_at": 1_800_000_000,
                "domain_hostname": "go.brand.com",
            },
            {"target_url": "bad"},
        ]
    }


def test_create_batch_accepts_plain_dicts(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.post("/v1/links/batch").mock(
        return_value=httpx.Response(
            200,
            json={
                "created": 1,
                "total": 1,
                "results": [{"index": 0, "ok": True, "code": "abc"}],
            },
        )
    )
    client.links.create_batch([{"target_url": "https://example.com/1"}])
    body = json.loads(route.calls.last.request.content)
    assert body == {"links": [{"target_url": "https://example.com/1"}]}


def test_create_batch_drops_non_batch_fields(mock_api: respx.MockRouter, client: Rerout) -> None:
    # Smart Link / SEO fields are not accepted by the batch endpoint and are
    # filtered out before sending.
    route = mock_api.post("/v1/links/batch").mock(
        return_value=httpx.Response(
            200,
            json={
                "created": 1,
                "total": 1,
                "results": [{"index": 0, "ok": True, "code": "abc"}],
            },
        )
    )
    client.links.create_batch(
        [
            CreateLinkInput(
                target_url="https://example.com/1",
                seo_title="ignored",
                password="ignored",
                track_conversions=True,
            )
        ]
    )
    body = json.loads(route.calls.last.request.content)
    assert body == {"links": [{"target_url": "https://example.com/1"}]}


def test_create_batch_empty_raises_without_request(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.post("/v1/links/batch").mock(
        return_value=httpx.Response(200, json=_batch_response())
    )
    with pytest.raises(ReroutError) as exc:
        client.links.create_batch([])
    assert exc.value.code == "bad_request"
    assert not route.called


def test_create_batch_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.post("/v1/links/batch").mock(
        return_value=httpx.Response(429, json={"code": "rate_limited", "message": "slow down"})
    )
    with pytest.raises(ReroutError) as exc:
        client.links.create_batch([CreateLinkInput(target_url="https://example.com/1")])
    assert exc.value.code == "rate_limited"
