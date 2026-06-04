"""Tests for the Smart Link fields on Link / CreateLinkInput / UpdateLinkInput.

Mirrors the links tests: a respx-mocked HTTP layer, no network access.
"""

from __future__ import annotations

import json

import httpx
import pytest
import respx

from conftest import sample_link
from rerout import (
    AbVariant,
    CreateLinkInput,
    Link,
    Rerout,
    ReroutError,
    RoutingRule,
    UpdateLinkInput,
)

# ─── Link response parsing ───────────────────────────────────────────────────


def test_link_parses_smart_link_fields(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.get("/v1/links/q4").mock(
        return_value=httpx.Response(
            200,
            json=sample_link(
                password_protected=True,
                max_clicks=100,
                click_count=42,
                track_conversions=True,
                routing_rules=[
                    {
                        "condition_type": "country",
                        "condition_op": "is",
                        "condition_value": "US",
                        "target_url": "https://example.com/us",
                    }
                ],
                ab_variants=[
                    {"id": 1, "target_url": "https://example.com/a", "weight": 70},
                    {"id": 2, "target_url": "https://example.com/b", "weight": 30},
                ],
            ),
        )
    )
    link = client.links.get("q4")
    assert isinstance(link, Link)
    assert link.password_protected is True
    assert link.max_clicks == 100
    assert link.click_count == 42
    assert link.track_conversions is True

    assert len(link.routing_rules) == 1
    rule = link.routing_rules[0]
    assert isinstance(rule, RoutingRule)
    assert rule.condition_type == "country"
    assert rule.condition_op == "is"
    assert rule.condition_value == "US"
    assert rule.target_url == "https://example.com/us"

    assert len(link.ab_variants) == 2
    variant = link.ab_variants[0]
    assert isinstance(variant, AbVariant)
    assert variant.id == 1
    assert variant.target_url == "https://example.com/a"
    assert variant.weight == 70


def test_link_smart_link_field_defaults_when_absent(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    # A legacy payload without the new keys parses with safe defaults.
    payload = sample_link()
    mock_api.get("/v1/links/q4").mock(return_value=httpx.Response(200, json=payload))
    link = client.links.get("q4")
    assert link.password_protected is False
    assert link.max_clicks is None
    assert link.click_count == 0
    assert link.track_conversions is False
    assert link.routing_rules == ()
    assert link.ab_variants == ()


def test_ab_variant_weight_defaults_to_one() -> None:
    variant = AbVariant.from_dict({"target_url": "https://example.com/a"})
    assert variant.weight == 1
    assert variant.id is None


# ─── CreateLinkInput payload ─────────────────────────────────────────────────


def test_create_payload_includes_smart_link_fields(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.post("/v1/links").mock(return_value=httpx.Response(200, json=sample_link()))
    client.links.create(
        CreateLinkInput(
            target_url="https://example.com",
            password="hunter2",
            max_clicks=500,
            track_conversions=True,
            routing_rules=[
                RoutingRule(
                    condition_type="device",
                    condition_op="is",
                    condition_value="mobile",
                    target_url="https://m.example.com",
                )
            ],
            ab_variants=[
                AbVariant(target_url="https://example.com/a", weight=2),
                AbVariant(target_url="https://example.com/b"),
            ],
        )
    )
    body = json.loads(route.calls.last.request.content)
    assert body["password"] == "hunter2"
    assert body["max_clicks"] == 500
    assert body["track_conversions"] is True
    assert body["routing_rules"] == [
        {
            "condition_type": "device",
            "condition_op": "is",
            "condition_value": "mobile",
            "target_url": "https://m.example.com",
        }
    ]
    # The server-assigned id is never sent; weight defaults to 1.
    assert body["ab_variants"] == [
        {"target_url": "https://example.com/a", "weight": 2},
        {"target_url": "https://example.com/b", "weight": 1},
    ]


def test_create_payload_omits_unset_smart_link_fields() -> None:
    payload = CreateLinkInput(target_url="https://example.com").to_payload()
    assert payload == {"target_url": "https://example.com"}
    for key in (
        "password",
        "max_clicks",
        "track_conversions",
        "routing_rules",
        "ab_variants",
    ):
        assert key not in payload


def test_create_payload_empty_collections_are_sent() -> None:
    payload = CreateLinkInput(
        target_url="https://example.com",
        routing_rules=[],
        ab_variants=[],
    ).to_payload()
    assert payload["routing_rules"] == []
    assert payload["ab_variants"] == []


# ─── UpdateLinkInput payload ─────────────────────────────────────────────────


def test_update_payload_includes_smart_link_fields() -> None:
    payload = UpdateLinkInput(
        password="new-secret",
        max_clicks=10,
        track_conversions=False,
        routing_rules=[
            RoutingRule(
                condition_type="country",
                condition_op="in",
                condition_value="US,CA",
                target_url="https://example.com/na",
            )
        ],
        ab_variants=[AbVariant(target_url="https://example.com/x", weight=5)],
    ).to_payload()
    assert payload["password"] == "new-secret"
    assert payload["max_clicks"] == 10
    assert payload["track_conversions"] is False
    assert payload["routing_rules"][0]["condition_op"] == "in"
    assert payload["ab_variants"] == [{"target_url": "https://example.com/x", "weight": 5}]


def test_update_clears_password_and_max_clicks_with_none() -> None:
    payload = UpdateLinkInput(password=None, max_clicks=None).to_payload()
    assert payload == {"password": None, "max_clicks": None}


def test_update_omits_unset_smart_link_fields() -> None:
    payload = UpdateLinkInput(is_active=True).to_payload()
    assert payload == {"is_active": True}


def test_update_full_replace_of_routing_rules(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.patch("/v1/links/q4").mock(
        return_value=httpx.Response(200, json=sample_link())
    )
    client.links.update("q4", UpdateLinkInput(routing_rules=[]))
    body = json.loads(route.calls.last.request.content)
    assert body == {"routing_rules": []}


def test_update_is_empty_with_only_smart_fields() -> None:
    assert UpdateLinkInput().is_empty is True
    assert UpdateLinkInput(track_conversions=True).is_empty is False
    assert UpdateLinkInput(routing_rules=[]).is_empty is False


def test_update_smart_links_round_trips(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.patch("/v1/links/q4").mock(
        return_value=httpx.Response(
            200,
            json=sample_link(
                password_protected=True,
                track_conversions=True,
                ab_variants=[{"id": 9, "target_url": "https://example.com/x", "weight": 5}],
            ),
        )
    )
    link = client.links.update(
        "q4",
        UpdateLinkInput(
            password="x",
            ab_variants=[AbVariant(target_url="https://example.com/x", weight=5)],
        ),
    )
    assert link.password_protected is True
    assert link.ab_variants[0].id == 9
    assert link.ab_variants[0].weight == 5


def test_update_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.patch("/v1/links/q4").mock(
        return_value=httpx.Response(400, json={"code": "bad_routing_rule", "message": "nope"})
    )
    with pytest.raises(ReroutError) as exc:
        client.links.update(
            "q4",
            UpdateLinkInput(
                routing_rules=[
                    RoutingRule(
                        condition_type="country",
                        condition_op="is",
                        condition_value="US",
                        target_url="https://example.com",
                    )
                ]
            ),
        )
    assert exc.value.code == "bad_routing_rule"
