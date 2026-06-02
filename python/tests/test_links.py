"""Tests for the links + project namespaces, plus URL-encoding edge cases."""

from __future__ import annotations

import httpx
import pytest
import respx

from conftest import sample_link
from rerout import (
    CreateLinkInput,
    Link,
    LinkStats,
    ListLinksResult,
    ProjectInfo,
    ProjectStats,
    Rerout,
    ReroutError,
    Tag,
    UpdateLinkInput,
)

# ─── links.create ──────────────────────────────────────────────────────────


def test_create_success(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.post("/v1/links").mock(
        return_value=httpx.Response(200, json=sample_link())
    )
    link = client.links.create(CreateLinkInput(target_url="https://example.com"))
    assert isinstance(link, Link)
    assert link.code == "q4"
    assert link.short_url == "https://go.brand.com/q4"
    assert link.is_active is True


def test_create_payload_includes_optional_fields(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.post("/v1/links").mock(
        return_value=httpx.Response(200, json=sample_link())
    )
    client.links.create(
        CreateLinkInput(
            target_url="https://example.com",
            domain_hostname="go.brand.com",
            code="q4",
            expires_at=1_800_000_000,
            seo_title="Title",
            seo_noindex=False,
        )
    )
    body = route.calls.last.request.content
    assert b'"domain_hostname"' in body
    assert b'"code"' in body
    assert b'"expires_at"' in body
    assert b'"seo_title"' in body
    assert b'"seo_noindex"' in body


def test_create_payload_omits_unset_fields() -> None:
    payload = CreateLinkInput(target_url="https://example.com").to_payload()
    assert payload == {"target_url": "https://example.com"}


def test_create_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.post("/v1/links").mock(
        return_value=httpx.Response(
            400, json={"code": "bad_target_url", "message": "nope"}
        )
    )
    with pytest.raises(ReroutError) as exc:
        client.links.create(CreateLinkInput(target_url="http://insecure"))
    assert exc.value.code == "bad_target_url"


# ─── links.list ────────────────────────────────────────────────────────────


def test_list_success(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links").mock(
        return_value=httpx.Response(
            200,
            json={
                "links": [sample_link(), sample_link(code="q5")],
                "next_cursor": 80,
            },
        )
    )
    result = client.links.list(limit=2)
    assert isinstance(result, ListLinksResult)
    assert len(result.links) == 2
    assert result.links[1].code == "q5"
    assert result.next_cursor == 80


def test_list_empty(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links").mock(
        return_value=httpx.Response(200, json={"links": [], "next_cursor": None})
    )
    result = client.links.list()
    assert result.links == ()
    assert result.next_cursor is None


def test_list_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links").mock(return_value=httpx.Response(401))
    with pytest.raises(ReroutError) as exc:
        client.links.list()
    assert exc.value.code == "unauthorized"


# ─── links.get ─────────────────────────────────────────────────────────────


def test_get_success(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links/q4").mock(
        return_value=httpx.Response(200, json=sample_link())
    )
    link = client.links.get("q4")
    assert link.code == "q4"


def test_get_parses_tags(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links/q4").mock(
        return_value=httpx.Response(
            200,
            json=sample_link(
                tags=[{"id": "tag_42", "name": "promo", "color": "#00ff00"}]
            ),
        )
    )
    link = client.links.get("q4")
    assert len(link.tags) == 1
    tag = link.tags[0]
    assert isinstance(tag, Tag)
    assert tag.id == "tag_42"
    assert tag.name == "promo"
    assert tag.color == "#00ff00"


def test_get_tags_default_empty_when_absent(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    payload = sample_link()
    del payload["tags"]
    mock_api.get("/v1/links/q4").mock(return_value=httpx.Response(200, json=payload))
    link = client.links.get("q4")
    assert link.tags == ()


def test_create_tags_empty_array(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.post("/v1/links").mock(
        return_value=httpx.Response(200, json=sample_link(tags=[]))
    )
    link = client.links.create(CreateLinkInput(target_url="https://example.com"))
    assert link.tags == ()


def test_get_not_found(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links/missing").mock(
        return_value=httpx.Response(
            404, json={"code": "not_found", "message": "no such link"}
        )
    )
    with pytest.raises(ReroutError) as exc:
        client.links.get("missing")
    assert exc.value.code == "not_found"
    assert exc.value.status == 404


# ─── links.update ──────────────────────────────────────────────────────────


def test_update_success(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.patch("/v1/links/q4").mock(
        return_value=httpx.Response(200, json=sample_link(is_active=False))
    )
    link = client.links.update("q4", UpdateLinkInput(is_active=False))
    assert link.is_active is False
    assert route.calls.last.request.content == b'{"is_active": false}'


def test_update_clears_field_with_none(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.patch("/v1/links/q4").mock(
        return_value=httpx.Response(200, json=sample_link())
    )
    client.links.update("q4", UpdateLinkInput(expires_at=None))
    assert route.calls.last.request.content == b'{"expires_at": null}'


def test_update_empty_input_raises_without_request(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.patch("/v1/links/q4").mock(
        return_value=httpx.Response(200, json=sample_link())
    )
    with pytest.raises(ReroutError) as exc:
        client.links.update("q4", UpdateLinkInput())
    assert exc.value.code == "bad_request"
    assert not route.called


def test_update_input_is_empty_property() -> None:
    assert UpdateLinkInput().is_empty is True
    assert UpdateLinkInput(is_active=True).is_empty is False


def test_update_payload_distinguishes_unset_from_none() -> None:
    # Only ``seo_title`` set to None — everything else untouched.
    payload = UpdateLinkInput(seo_title=None).to_payload()
    assert payload == {"seo_title": None}


def test_update_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.patch("/v1/links/q4").mock(
        return_value=httpx.Response(
            403, json={"code": "forbidden", "message": "not yours"}
        )
    )
    with pytest.raises(ReroutError) as exc:
        client.links.update("q4", UpdateLinkInput(target_url="https://new.example"))
    assert exc.value.code == "forbidden"


# ─── links.delete ──────────────────────────────────────────────────────────


def test_delete_returns_true(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.delete("/v1/links/q4").mock(
        return_value=httpx.Response(200, json={"deleted": True})
    )
    assert client.links.delete("q4") is True


def test_delete_respects_false_flag(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.delete("/v1/links/q4").mock(
        return_value=httpx.Response(200, json={"deleted": False})
    )
    assert client.links.delete("q4") is False


def test_delete_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.delete("/v1/links/q4").mock(return_value=httpx.Response(404))
    with pytest.raises(ReroutError) as exc:
        client.links.delete("q4")
    assert exc.value.code == "not_found"


# ─── links.stats ───────────────────────────────────────────────────────────


def test_stats_success(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links/q4/stats").mock(
        return_value=httpx.Response(
            200,
            json={
                "code": "q4",
                "days": 30,
                "total_clicks": 120,
                "qr_scans": 8,
                "countries": [{"value": "US", "clicks": 90}],
                "referrers": [{"value": "twitter.com", "clicks": 30}],
            },
        )
    )
    stats = client.links.stats("q4")
    assert isinstance(stats, LinkStats)
    assert stats.total_clicks == 120
    assert stats.countries[0].value == "US"
    assert stats.countries[0].clicks == 90


def test_stats_default_days(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.get("/v1/links/q4/stats").mock(
        return_value=httpx.Response(
            200,
            json={
                "code": "q4",
                "days": 30,
                "total_clicks": 0,
                "qr_scans": 0,
                "countries": [],
                "referrers": [],
            },
        )
    )
    client.links.stats("q4")
    assert route.calls.last.request.url.params["days"] == "30"


def test_stats_custom_days(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.get("/v1/links/q4/stats").mock(
        return_value=httpx.Response(
            200,
            json={
                "code": "q4",
                "days": 7,
                "total_clicks": 0,
                "qr_scans": 0,
                "countries": [],
                "referrers": [],
            },
        )
    )
    client.links.stats("q4", days=7)
    assert route.calls.last.request.url.params["days"] == "7"


def test_stats_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links/q4/stats").mock(return_value=httpx.Response(500))
    with pytest.raises(ReroutError) as exc:
        client.links.stats("q4")
    assert exc.value.code == "server_error"


# ─── project ───────────────────────────────────────────────────────────────


def test_project_stats_success(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/projects/me/stats").mock(
        return_value=httpx.Response(
            200,
            json={
                "days": 30,
                "total_clicks": 500,
                "qr_scans": 40,
                "daily": [{"day": 1_700_000_000, "clicks": 10, "qr_scans": 2}],
                "countries": [{"value": "ZA", "clicks": 200}],
                "referrers": [],
                "devices": [{"value": "mobile", "clicks": 300}],
                "browsers": [],
                "top_codes": [{"value": "q4", "clicks": 120}],
            },
        )
    )
    stats = client.project.stats()
    assert isinstance(stats, ProjectStats)
    assert stats.total_clicks == 500
    assert stats.daily[0].clicks == 10
    assert stats.daily[0].qr_scans == 2
    assert stats.devices[0].value == "mobile"
    assert stats.top_codes[0].value == "q4"


def test_project_stats_error_path(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.get("/v1/projects/me/stats").mock(return_value=httpx.Response(429))
    with pytest.raises(ReroutError) as exc:
        client.project.stats()
    assert exc.value.code == "rate_limited"


def test_project_me_success(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/projects/me").mock(
        return_value=httpx.Response(
            200, json={"id": "proj_123", "name": "Brand", "slug": "brand"}
        )
    )
    info = client.project.me()
    assert isinstance(info, ProjectInfo)
    assert info.id == "proj_123"
    assert info.name == "Brand"
    assert info.slug == "brand"


def test_project_me_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/projects/me").mock(return_value=httpx.Response(401))
    with pytest.raises(ReroutError) as exc:
        client.project.me()
    assert exc.value.code == "unauthorized"


# ─── URL-encoding edge cases ───────────────────────────────────────────────


@pytest.mark.parametrize(
    ("code", "encoded"),
    [
        ("hello world", "hello%20world"),
        ("a+b", "a%2Bb"),
        ("café", "caf%C3%A9"),
        ("go/promo", "go%2Fpromo"),
    ],
)
def test_get_url_encodes_code(
    mock_api: respx.MockRouter, client: Rerout, code: str, encoded: str
) -> None:
    route = mock_api.get(f"/v1/links/{encoded}").mock(
        return_value=httpx.Response(200, json=sample_link(code=code))
    )
    client.links.get(code)
    assert route.called
    assert encoded in str(route.calls.last.request.url)


def test_stats_path_encodes_code(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.get("/v1/links/go%2Fpromo/stats").mock(
        return_value=httpx.Response(
            200,
            json={
                "code": "go/promo",
                "days": 30,
                "total_clicks": 0,
                "qr_scans": 0,
                "countries": [],
                "referrers": [],
            },
        )
    )
    client.links.stats("go/promo")
    assert route.called
