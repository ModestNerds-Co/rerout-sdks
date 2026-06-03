"""Tests for the webhooks management namespace (create / list / delete).

Mirrors the links tests: a respx-mocked HTTP layer, no network access. The
inbound signature-verification helper is covered separately in
``test_webhooks.py`` and is untouched here.
"""

from __future__ import annotations

import json

import httpx
import pytest
import respx

from rerout import (
    CreatedWebhook,
    CreateWebhookInput,
    ListWebhooksResult,
    Rerout,
    ReroutError,
    Webhook,
)


def sample_webhook(**overrides: object) -> dict[str, object]:
    """A complete ``Webhook`` JSON payload, with optional field overrides."""
    base: dict[str, object] = {
        "id": "wh_abc123",
        "project_id": "prj_test",
        "name": "Order events",
        "url": "https://example.com/hooks/rerout",
        "events": ["link.created", "link.clicked"],
        "is_active": True,
        "payload_format": "json",
        "created_at": 1_700_000_000,
        "updated_at": 1_700_000_000,
        "last_delivery_at": None,
        "last_success_at": None,
        "last_failure_at": None,
    }
    base.update(overrides)
    return base


# ─── webhooks.create ───────────────────────────────────────────────────────


def test_create_posts_and_returns_signing_secret(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.post("/v1/projects/me/webhooks").mock(
        return_value=httpx.Response(
            201,
            json={
                "endpoint": sample_webhook(),
                "signing_secret": "whsec_supersecret",
            },
        )
    )
    result = client.webhooks.create(
        CreateWebhookInput(
            name="Order events",
            url="https://example.com/hooks/rerout",
            events=["link.created", "link.clicked"],
        )
    )
    assert isinstance(result, CreatedWebhook)
    assert result.signing_secret == "whsec_supersecret"
    assert isinstance(result.endpoint, Webhook)
    assert result.endpoint.id == "wh_abc123"
    assert result.endpoint.events == ("link.created", "link.clicked")

    body = json.loads(route.calls.last.request.content)
    assert body == {
        "name": "Order events",
        "url": "https://example.com/hooks/rerout",
        "events": ["link.created", "link.clicked"],
    }


def test_create_omits_unset_optional_fields() -> None:
    payload = CreateWebhookInput(
        name="Order events",
        url="https://example.com/hooks/rerout",
        events=["link.created"],
    ).to_payload()
    assert payload == {
        "name": "Order events",
        "url": "https://example.com/hooks/rerout",
        "events": ["link.created"],
    }


def test_create_forwards_is_active_and_payload_format(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.post("/v1/projects/me/webhooks").mock(
        return_value=httpx.Response(
            201,
            json={
                "endpoint": sample_webhook(is_active=False, payload_format="slack"),
                "signing_secret": "whsec_x",
            },
        )
    )
    result = client.webhooks.create(
        CreateWebhookInput(
            name="Slack",
            url="https://hooks.slack.com/services/T/B/x",
            events=["link.created"],
            is_active=False,
            payload_format="slack",
        )
    )
    body = json.loads(route.calls.last.request.content)
    assert body["is_active"] is False
    assert body["payload_format"] == "slack"
    assert result.endpoint.payload_format == "slack"
    assert result.endpoint.is_active is False


def test_create_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.post("/v1/projects/me/webhooks").mock(
        return_value=httpx.Response(
            400, json={"code": "invalid_webhook_url", "message": "nope"}
        )
    )
    with pytest.raises(ReroutError) as exc:
        client.webhooks.create(
            CreateWebhookInput(
                name="bad",
                url="http://insecure",
                events=["link.created"],
            )
        )
    assert exc.value.code == "invalid_webhook_url"


# ─── webhooks.list ─────────────────────────────────────────────────────────


def test_list_parses_endpoints_and_event_types(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.get("/v1/projects/me/webhooks").mock(
        return_value=httpx.Response(
            200,
            json={
                "endpoints": [sample_webhook()],
                "event_types": ["link.created", "link.clicked", "domain.verified"],
            },
        )
    )
    result = client.webhooks.list()
    assert isinstance(result, ListWebhooksResult)
    assert len(result.endpoints) == 1
    assert result.endpoints[0].url == "https://example.com/hooks/rerout"
    assert "domain.verified" in result.event_types
    assert route.calls.last.request.method == "GET"


def test_list_empty(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/projects/me/webhooks").mock(
        return_value=httpx.Response(200, json={"endpoints": [], "event_types": []})
    )
    result = client.webhooks.list()
    assert result.endpoints == ()
    assert result.event_types == ()


def test_list_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/projects/me/webhooks").mock(
        return_value=httpx.Response(401)
    )
    with pytest.raises(ReroutError) as exc:
        client.webhooks.list()
    assert exc.value.code == "unauthorized"


# ─── webhooks.delete ───────────────────────────────────────────────────────


def test_delete_returns_true(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.delete("/v1/projects/me/webhooks/wh_abc123").mock(
        return_value=httpx.Response(200, json={"deleted": True})
    )
    assert client.webhooks.delete("wh_abc123") is True
    assert route.called
    assert route.calls.last.request.method == "DELETE"


def test_delete_respects_false_flag(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    mock_api.delete("/v1/projects/me/webhooks/wh_abc123").mock(
        return_value=httpx.Response(200, json={"deleted": False})
    )
    assert client.webhooks.delete("wh_abc123") is False


def test_delete_url_encodes_endpoint_id(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.delete("/v1/projects/me/webhooks/wh%2Fweird%20id").mock(
        return_value=httpx.Response(200, json={"deleted": True})
    )
    client.webhooks.delete("wh/weird id")
    assert route.called
    assert "wh%2Fweird%20id" in str(route.calls.last.request.url)


def test_delete_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.delete("/v1/projects/me/webhooks/wh_abc123").mock(
        return_value=httpx.Response(404)
    )
    with pytest.raises(ReroutError) as exc:
        client.webhooks.delete("wh_abc123")
    assert exc.value.code == "not_found"
