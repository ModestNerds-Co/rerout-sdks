"""Tests for the conversions namespace (record).

Mirrors the links tests: a respx-mocked HTTP layer, no network access.
"""

from __future__ import annotations

import json

import httpx
import pytest
import respx

from rerout import RecordedConversion, Rerout, ReroutError


def test_record_posts_minimal_body(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.post("/v1/conversions").mock(
        return_value=httpx.Response(200, json={"recorded": True, "duplicate": False})
    )
    result = client.conversions.record("clk_123", "purchase")
    assert isinstance(result, RecordedConversion)
    assert result.recorded is True
    assert result.duplicate is False

    body = json.loads(route.calls.last.request.content)
    assert body == {"click_id": "clk_123", "event_name": "purchase"}


def test_record_forwards_optional_value_and_currency(
    mock_api: respx.MockRouter, client: Rerout
) -> None:
    route = mock_api.post("/v1/conversions").mock(
        return_value=httpx.Response(200, json={"recorded": True, "duplicate": False})
    )
    client.conversions.record("clk_123", "purchase", value_cents=4999, currency="USD")
    body = json.loads(route.calls.last.request.content)
    assert body == {
        "click_id": "clk_123",
        "event_name": "purchase",
        "value_cents": 4999,
        "currency": "USD",
    }


def test_record_omits_none_optional_fields(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.post("/v1/conversions").mock(
        return_value=httpx.Response(200, json={"recorded": True, "duplicate": False})
    )
    client.conversions.record("clk_123", "signup", value_cents=None, currency=None)
    body = json.loads(route.calls.last.request.content)
    assert "value_cents" not in body
    assert "currency" not in body


def test_record_reports_duplicate(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.post("/v1/conversions").mock(
        return_value=httpx.Response(200, json={"recorded": False, "duplicate": True})
    )
    result = client.conversions.record("clk_123", "purchase")
    assert result.recorded is False
    assert result.duplicate is True


def test_record_targets_v1_conversions(mock_api: respx.MockRouter, client: Rerout) -> None:
    route = mock_api.post("/v1/conversions").mock(
        return_value=httpx.Response(200, json={"recorded": True, "duplicate": False})
    )
    client.conversions.record("clk_123", "purchase")
    assert route.calls.last.request.url.path == "/v1/conversions"
    assert route.calls.last.request.method == "POST"


def test_record_error_path(mock_api: respx.MockRouter, client: Rerout) -> None:
    mock_api.post("/v1/conversions").mock(
        return_value=httpx.Response(
            404, json={"code": "click_not_found", "message": "no such click"}
        )
    )
    with pytest.raises(ReroutError) as exc:
        client.conversions.record("clk_missing", "purchase")
    assert exc.value.code == "click_not_found"
