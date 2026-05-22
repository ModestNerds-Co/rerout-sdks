"""Tests for signal dispatch from ``WebhookView``."""

from __future__ import annotations

import json
import time
from collections.abc import Iterator
from typing import Any

import pytest
from django.test import Client

from conftest import TEST_WEBHOOK_SECRET, sign_body
from rerout_django import (
    WebhookView,
    rerout_link_clicked,
    rerout_link_created,
    rerout_link_deleted,
    rerout_link_updated,
    rerout_qr_scanned,
    rerout_webhook_received,
)
from rerout_django.signals import EVENT_SIGNALS

WEBHOOK_URL = "/rerout/webhook/"


class SignalRecorder:
    """Captures the kwargs every time a connected signal fires."""

    def __init__(self) -> None:
        self.calls: list[dict[str, Any]] = []

    def __call__(self, sender: Any, **kwargs: Any) -> None:
        self.calls.append({"sender": sender, **kwargs})


@pytest.fixture
def http() -> Client:
    return Client()


def _post(http: Client, payload: dict[str, Any]) -> Any:
    body = json.dumps(payload)
    ts = int(time.time())
    header = sign_body(body, TEST_WEBHOOK_SECRET, ts)
    return http.post(
        WEBHOOK_URL,
        data=body,
        content_type="application/json",
        HTTP_X_REROUT_SIGNATURE=header,
    )


@pytest.fixture
def catch_all() -> Iterator[SignalRecorder]:
    recorder = SignalRecorder()
    rerout_webhook_received.connect(recorder, weak=False)
    yield recorder
    rerout_webhook_received.disconnect(recorder)


# ─── catch-all signal ──────────────────────────────────────────────────────


def test_catch_all_fires_for_known_event(
    http: Client, catch_all: SignalRecorder
) -> None:
    _post(http, {"event": "link.clicked", "code": "q4"})
    assert len(catch_all.calls) == 1
    call = catch_all.calls[0]
    assert call["event"] == "link.clicked"
    assert call["payload"] == {"event": "link.clicked", "code": "q4"}
    assert call["sender"] is WebhookView
    assert call["request"] is not None


def test_catch_all_fires_for_unknown_event(
    http: Client, catch_all: SignalRecorder
) -> None:
    _post(http, {"event": "brand.new.event"})
    assert len(catch_all.calls) == 1
    assert catch_all.calls[0]["event"] == "brand.new.event"


def test_catch_all_fires_with_empty_event(
    http: Client, catch_all: SignalRecorder
) -> None:
    _post(http, {"no_event": True})
    assert len(catch_all.calls) == 1
    assert catch_all.calls[0]["event"] == ""


def test_catch_all_not_fired_on_bad_signature(
    http: Client, catch_all: SignalRecorder
) -> None:
    body = json.dumps({"event": "link.clicked"})
    http.post(
        WEBHOOK_URL,
        data=body,
        content_type="application/json",
        HTTP_X_REROUT_SIGNATURE="garbage",
    )
    assert catch_all.calls == []


def test_catch_all_not_fired_on_bad_body(
    http: Client, catch_all: SignalRecorder
) -> None:
    body = "not json"
    ts = int(time.time())
    header = sign_body(body, TEST_WEBHOOK_SECRET, ts)
    http.post(
        WEBHOOK_URL,
        data=body,
        content_type="application/json",
        HTTP_X_REROUT_SIGNATURE=header,
    )
    assert catch_all.calls == []


# ─── event-specific signals ────────────────────────────────────────────────


@pytest.mark.parametrize(
    ("event", "signal"),
    [
        ("link.created", rerout_link_created),
        ("link.updated", rerout_link_updated),
        ("link.deleted", rerout_link_deleted),
        ("link.clicked", rerout_link_clicked),
        ("qr.scanned", rerout_qr_scanned),
    ],
)
def test_specific_signal_fires_for_its_event(
    http: Client, event: str, signal: Any
) -> None:
    recorder = SignalRecorder()
    signal.connect(recorder, weak=False)
    try:
        _post(http, {"event": event, "code": "q4"})
    finally:
        signal.disconnect(recorder)
    assert len(recorder.calls) == 1
    assert recorder.calls[0]["event"] == event
    assert recorder.calls[0]["payload"]["code"] == "q4"


def test_specific_signal_does_not_fire_for_other_event(http: Client) -> None:
    recorder = SignalRecorder()
    rerout_link_created.connect(recorder, weak=False)
    try:
        _post(http, {"event": "link.clicked"})
    finally:
        rerout_link_created.disconnect(recorder)
    assert recorder.calls == []


def test_no_specific_signal_for_unknown_event(http: Client) -> None:
    # Connect every specific signal; an unknown event should fire none of them.
    recorder = SignalRecorder()
    for signal in EVENT_SIGNALS.values():
        signal.connect(recorder, weak=False)
    try:
        _post(http, {"event": "totally.unknown"})
    finally:
        for signal in EVENT_SIGNALS.values():
            signal.disconnect(recorder)
    assert recorder.calls == []


def test_catch_all_and_specific_both_fire(
    http: Client, catch_all: SignalRecorder
) -> None:
    specific = SignalRecorder()
    rerout_link_clicked.connect(specific, weak=False)
    try:
        _post(http, {"event": "link.clicked"})
    finally:
        rerout_link_clicked.disconnect(specific)
    assert len(catch_all.calls) == 1
    assert len(specific.calls) == 1


def test_event_signals_map_is_complete() -> None:
    assert set(EVENT_SIGNALS) == {
        "link.created",
        "link.updated",
        "link.deleted",
        "link.clicked",
        "qr.scanned",
    }
