"""Tests for the tags management namespace (list / create / update / delete).

Mirrors ``typescript/test/tags.test.ts`` and the webhooks-management tests: a
respx-mocked HTTP layer, no network access.
"""

from __future__ import annotations

import json

import httpx
import pytest
import respx

from rerout import (
    CreateTagInput,
    ListTagsResult,
    Rerout,
    ReroutError,
    Tag,
    TagSummary,
    UpdateTagInput,
)

SAMPLE_TAG: dict[str, object] = {"id": "tag_abc123", "name": "Spring 2026", "color": "teal"}
SAMPLE_SUMMARY: dict[str, object] = {**SAMPLE_TAG, "link_count": 4}


# ─── tags.list ─────────────────────────────────────────────────────────────


def test_list_gets_tags_and_returns_link_counts(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.get("/v1/projects/me/tags").mock(
        return_value=httpx.Response(200, json={"tags": [SAMPLE_SUMMARY]})
    )
    result = client.tags.list()
    assert isinstance(result, ListTagsResult)
    assert len(result.tags) == 1
    assert isinstance(result.tags[0], TagSummary)
    assert result.tags[0].link_count == 4
    assert result.tags[0].id == "tag_abc123"
    assert route.calls.last.request.method == "GET"


def test_list_empty(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/projects/me/tags").mock(return_value=httpx.Response(200, json={"tags": []}))
    result = client.tags.list()
    assert result.tags == ()


def test_list_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/projects/me/tags").mock(return_value=httpx.Response(401))
    with pytest.raises(ReroutError) as exc:
        client.tags.list()
    assert exc.value.code == "unauthorized"


# ─── tags.create ───────────────────────────────────────────────────────────


def test_create_posts_name_and_color(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.post("/v1/projects/me/tags").mock(
        return_value=httpx.Response(201, json=SAMPLE_TAG)
    )
    tag = client.tags.create(CreateTagInput(name="Spring 2026", color="teal"))
    assert isinstance(tag, Tag)
    assert tag.id == "tag_abc123"

    body = json.loads(route.calls.last.request.content)
    assert body == {"name": "Spring 2026", "color": "teal"}
    assert route.calls.last.request.method == "POST"


def test_create_omits_unset_color() -> None:
    assert CreateTagInput(name="Spring 2026").to_payload() == {"name": "Spring 2026"}


def test_create_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.post("/v1/projects/me/tags").mock(
        return_value=httpx.Response(400, json={"code": "invalid_color", "message": "nope"})
    )
    with pytest.raises(ReroutError) as exc:
        client.tags.create(CreateTagInput(name="x", color="chartreuse"))
    assert exc.value.code == "invalid_color"


# ─── tags.update ───────────────────────────────────────────────────────────


def test_update_patches_tag_by_id(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.patch("/v1/projects/me/tags/tag_abc123").mock(
        return_value=httpx.Response(200, json={**SAMPLE_TAG, "color": "red"})
    )
    tag = client.tags.update("tag_abc123", UpdateTagInput(color="red"))
    assert tag.color == "red"

    body = json.loads(route.calls.last.request.content)
    assert body == {"color": "red"}
    assert route.calls.last.request.method == "PATCH"


def test_update_forwards_only_provided_fields(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.patch("/v1/projects/me/tags/tag_abc123").mock(
        return_value=httpx.Response(200, json={**SAMPLE_TAG, "name": "Renamed"})
    )
    tag = client.tags.update("tag_abc123", UpdateTagInput(name="Renamed"))
    assert tag.name == "Renamed"

    body = json.loads(route.calls.last.request.content)
    assert body == {"name": "Renamed"}


def test_update_url_encodes_tag_id(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.patch("/v1/projects/me/tags/tag%2Fweird%20id").mock(
        return_value=httpx.Response(200, json=SAMPLE_TAG)
    )
    client.tags.update("tag/weird id", UpdateTagInput(color="teal"))
    assert route.called
    assert "tag%2Fweird%20id" in str(route.calls.last.request.url)


def test_update_empty_payload_sends_empty_body() -> None:
    # No client-side guard (unlike links.update) — mirrors the TS reference.
    assert UpdateTagInput().to_payload() == {}


def test_update_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.patch("/v1/projects/me/tags/tag_abc123").mock(return_value=httpx.Response(404))
    with pytest.raises(ReroutError) as exc:
        client.tags.update("tag_abc123", UpdateTagInput(name="x"))
    assert exc.value.code == "not_found"


# ─── tags.delete ───────────────────────────────────────────────────────────


def test_delete_sends_delete_and_returns_true(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.delete("/v1/projects/me/tags/tag_abc123").mock(
        return_value=httpx.Response(200, json={"deleted": True})
    )
    assert client.tags.delete("tag_abc123") is True
    assert route.called
    assert route.calls.last.request.method == "DELETE"


def test_delete_respects_false_flag(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.delete("/v1/projects/me/tags/tag_abc123").mock(
        return_value=httpx.Response(200, json={"deleted": False})
    )
    assert client.tags.delete("tag_abc123") is False


def test_delete_url_encodes_tag_id(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.delete("/v1/projects/me/tags/tag%2Fweird%20id").mock(
        return_value=httpx.Response(200, json={"deleted": True})
    )
    client.tags.delete("tag/weird id")
    assert route.called
    assert "tag%2Fweird%20id" in str(route.calls.last.request.url)


def test_delete_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.delete("/v1/projects/me/tags/tag_abc123").mock(return_value=httpx.Response(404))
    with pytest.raises(ReroutError) as exc:
        client.tags.delete("tag_abc123")
    assert exc.value.code == "not_found"


# ─── namespace wiring ──────────────────────────────────────────────────────


def test_tags_namespace_present(client: Rerout) -> None:
    from rerout import Tags

    assert isinstance(client.tags, Tags)
