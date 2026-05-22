"""Shared fixtures + helpers for the rerout test suite."""

from __future__ import annotations

from collections.abc import Iterator

import httpx
import pytest
import respx

from rerout import Rerout

TEST_BASE_URL = "https://api.test.rerout.co"
TEST_API_KEY = "rrk_test_key"


@pytest.fixture
def mock_api() -> Iterator[respx.MockRouter]:
    """A respx router scoped to the test base URL.

    ``assert_all_called`` is left off so individual tests can register more
    routes than they exercise without failing.
    """
    with respx.mock(base_url=TEST_BASE_URL, assert_all_called=False) as router:
        yield router


@pytest.fixture
def client() -> Iterator[Rerout]:
    """A :class:`Rerout` pointed at the mocked test base URL."""
    rerout = Rerout(api_key=TEST_API_KEY, base_url=TEST_BASE_URL)
    yield rerout
    rerout.close()


def sample_link(**overrides: object) -> dict[str, object]:
    """A complete ``Link`` JSON payload, with optional field overrides."""
    base: dict[str, object] = {
        "code": "q4",
        "short_url": "https://go.brand.com/q4",
        "target_url": "https://example.com/q4-sale",
        "project_id": "proj_123",
        "is_active": True,
        "seo_noindex": True,
        "created_at": 1_700_000_000,
        "updated_at": 1_700_000_100,
        "domain_hostname": "go.brand.com",
        "expires_at": None,
        "seo_title": "Q4 Sale",
        "seo_description": None,
        "seo_image_url": None,
        "seo_canonical_url": None,
        "seo_updated_at": None,
    }
    base.update(overrides)
    return base


def json_response(payload: object, status: int = 200) -> httpx.Response:
    """Build an ``httpx.Response`` carrying a JSON body."""
    return httpx.Response(status, json=payload)
