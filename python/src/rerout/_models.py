"""Typed dataclasses for objects returned by the Rerout API.

The shapes mirror the server-side JSON responses verbatim — JSON keys map to
attribute names of the same name (snake_case).

Input dataclasses (``CreateLinkInput``, ``UpdateLinkInput``) are also defined
here. ``UpdateLinkInput`` uses an ``Unset`` sentinel so the caller can
distinguish "leave this field alone" from "clear this field on the server"
(by passing ``None``).
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Final, Literal, TypeAlias, TypeVar


class _UnsetType:
    """Sentinel singleton meaning "this field was not provided".

    The :class:`UpdateLinkInput` dataclass uses this to differentiate between
    a caller passing ``None`` (clear the field on the server) and not passing
    a value at all (leave the field untouched).
    """

    _instance: _UnsetType | None = None

    def __new__(cls) -> _UnsetType:
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __repr__(self) -> str:
        return "UNSET"

    def __bool__(self) -> bool:
        return False


UNSET: Final[_UnsetType] = _UnsetType()
"""Singleton sentinel used as the default for optional update-input fields."""


_T = TypeVar("_T")

# Type alias for "value, or omitted". Written as a ``TypeAlias`` over a
# ``TypeVar`` rather than the PEP 695 ``type Maybe[T]`` statement so the
# package stays importable on Python 3.10 and 3.11.
Maybe: TypeAlias = "_T | _UnsetType"


EccLevel = Literal["L", "M", "Q", "H"]

ConditionType = Literal["country", "device"]
"""The visitor attribute a Smart Link routing rule matches against."""

ConditionOp = Literal["is", "is_not", "in"]
"""How a routing rule compares the visitor attribute to its value."""


@dataclass(frozen=True, slots=True)
class RoutingRule:
    """A Smart Link routing rule: send matching visitors to ``target_url``.

    ``condition_type`` is the visitor attribute to match (``country`` or
    ``device``); ``condition_op`` is the comparison (``is`` / ``is_not`` /
    ``in``); ``condition_value`` is the value to compare against (e.g. ``"US"``
    or ``"US,CA,GB"`` for ``in``).
    """

    condition_type: str
    condition_op: str
    condition_value: str
    target_url: str

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> RoutingRule:
        """Build a ``RoutingRule`` from the raw JSON dict the API returns."""
        return cls(
            condition_type=str(data["condition_type"]),
            condition_op=str(data["condition_op"]),
            condition_value=str(data["condition_value"]),
            target_url=str(data["target_url"]),
        )

    def to_payload(self) -> dict[str, Any]:
        """Render as the JSON dict the API expects on create/update."""
        return {
            "condition_type": self.condition_type,
            "condition_op": self.condition_op,
            "condition_value": self.condition_value,
            "target_url": self.target_url,
        }


@dataclass(frozen=True, slots=True)
class AbVariant:
    """One A/B-test variant attached to a Smart Link.

    ``id`` is assigned server-side and is read-only — it is absent from the
    create/update payload. ``weight`` controls the relative traffic share and
    defaults to ``1`` server-side.
    """

    target_url: str
    weight: int = 1
    id: int | None = None

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> AbVariant:
        """Build an ``AbVariant`` from the raw JSON dict the API returns."""
        return cls(
            target_url=str(data["target_url"]),
            weight=int(data.get("weight", 1)),
            id=_opt_int(data.get("id")),
        )

    def to_payload(self) -> dict[str, Any]:
        """Render as the JSON dict the API expects on create/update.

        The server-assigned ``id`` is never sent.
        """
        return {"target_url": self.target_url, "weight": self.weight}


@dataclass(frozen=True, slots=True)
class Tag:
    """A tag attached to a link, as returned by the Rerout API.

    Tags are read-only for API-key clients: they appear on link responses but
    cannot be written through ``CreateLinkInput`` / ``UpdateLinkInput``.
    """

    id: str
    name: str
    color: str

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> Tag:
        """Build a ``Tag`` from the raw JSON dict the API returns."""
        return cls(
            id=str(data["id"]),
            name=str(data["name"]),
            color=str(data["color"]),
        )


@dataclass(frozen=True, slots=True)
class Link:
    """A short link as returned by the Rerout API."""

    code: str
    short_url: str
    target_url: str
    project_id: str
    is_active: bool
    seo_noindex: bool
    created_at: int
    updated_at: int
    password_protected: bool = False
    click_count: int = 0
    track_conversions: bool = False
    max_clicks: int | None = None
    domain_hostname: str | None = None
    expires_at: int | None = None
    seo_title: str | None = None
    seo_description: str | None = None
    seo_image_url: str | None = None
    seo_canonical_url: str | None = None
    seo_updated_at: int | None = None
    tags: tuple[Tag, ...] = ()
    routing_rules: tuple[RoutingRule, ...] = ()
    ab_variants: tuple[AbVariant, ...] = ()

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> Link:
        """Build a ``Link`` from the raw JSON dict the API returns."""
        return cls(
            code=str(data["code"]),
            short_url=str(data["short_url"]),
            target_url=str(data["target_url"]),
            project_id=str(data["project_id"]),
            is_active=bool(data["is_active"]),
            seo_noindex=bool(data.get("seo_noindex", True)),
            created_at=int(data["created_at"]),
            updated_at=int(data["updated_at"]),
            password_protected=bool(data.get("password_protected", False)),
            click_count=int(data.get("click_count", 0)),
            track_conversions=bool(data.get("track_conversions", False)),
            max_clicks=_opt_int(data.get("max_clicks")),
            domain_hostname=_opt_str(data.get("domain_hostname")),
            expires_at=_opt_int(data.get("expires_at")),
            seo_title=_opt_str(data.get("seo_title")),
            seo_description=_opt_str(data.get("seo_description")),
            seo_image_url=_opt_str(data.get("seo_image_url")),
            seo_canonical_url=_opt_str(data.get("seo_canonical_url")),
            seo_updated_at=_opt_int(data.get("seo_updated_at")),
            tags=tuple(Tag.from_dict(t) for t in data.get("tags", []) or []),
            routing_rules=tuple(
                RoutingRule.from_dict(r) for r in data.get("routing_rules", []) or []
            ),
            ab_variants=tuple(AbVariant.from_dict(v) for v in data.get("ab_variants", []) or []),
        )


@dataclass(frozen=True, slots=True)
class ListLinksResult:
    """Paginated list of links returned by ``GET /v1/links``."""

    links: tuple[Link, ...]
    next_cursor: int | None

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> ListLinksResult:
        raw_links = data.get("links", []) or []
        return cls(
            links=tuple(Link.from_dict(item) for item in raw_links),
            next_cursor=_opt_int(data.get("next_cursor")),
        )


@dataclass(frozen=True, slots=True)
class StatsBreakdown:
    """A single bucket in a categorical click breakdown."""

    value: str
    clicks: int

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> StatsBreakdown:
        return cls(value=str(data["value"]), clicks=int(data["clicks"]))


@dataclass(frozen=True, slots=True)
class DailyClicksPoint:
    """One day of click + QR scan activity."""

    day: int
    clicks: int
    qr_scans: int

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> DailyClicksPoint:
        return cls(
            day=int(data["day"]),
            clicks=int(data["clicks"]),
            qr_scans=int(data["qr_scans"]),
        )


@dataclass(frozen=True, slots=True)
class ProjectStats:
    """Aggregate click + QR stats across the entire project."""

    days: int
    total_clicks: int
    qr_scans: int
    daily: tuple[DailyClicksPoint, ...]
    countries: tuple[StatsBreakdown, ...]
    referrers: tuple[StatsBreakdown, ...]
    devices: tuple[StatsBreakdown, ...]
    browsers: tuple[StatsBreakdown, ...]
    top_codes: tuple[StatsBreakdown, ...]

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> ProjectStats:
        return cls(
            days=int(data["days"]),
            total_clicks=int(data["total_clicks"]),
            qr_scans=int(data["qr_scans"]),
            daily=tuple(DailyClicksPoint.from_dict(d) for d in data.get("daily", []) or []),
            countries=tuple(StatsBreakdown.from_dict(d) for d in data.get("countries", []) or []),
            referrers=tuple(StatsBreakdown.from_dict(d) for d in data.get("referrers", []) or []),
            devices=tuple(StatsBreakdown.from_dict(d) for d in data.get("devices", []) or []),
            browsers=tuple(StatsBreakdown.from_dict(d) for d in data.get("browsers", []) or []),
            top_codes=tuple(StatsBreakdown.from_dict(d) for d in data.get("top_codes", []) or []),
        )


@dataclass(frozen=True, slots=True)
class LinkStats:
    """Click + QR stats for a single short link."""

    code: str
    days: int
    total_clicks: int
    qr_scans: int
    countries: tuple[StatsBreakdown, ...]
    referrers: tuple[StatsBreakdown, ...]

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> LinkStats:
        return cls(
            code=str(data["code"]),
            days=int(data["days"]),
            total_clicks=int(data["total_clicks"]),
            qr_scans=int(data["qr_scans"]),
            countries=tuple(StatsBreakdown.from_dict(d) for d in data.get("countries", []) or []),
            referrers=tuple(StatsBreakdown.from_dict(d) for d in data.get("referrers", []) or []),
        )


@dataclass(frozen=True, slots=True)
class ProjectInfo:
    """Identifying info for the project that owns the current API key."""

    id: str
    name: str
    slug: str

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> ProjectInfo:
        return cls(
            id=str(data["id"]),
            name=str(data["name"]),
            slug=str(data["slug"]),
        )


@dataclass(frozen=True, slots=True)
class RecordedConversion:
    """Result of recording a conversion via ``POST /v1/conversions``.

    ``recorded`` is ``True`` when the conversion was stored. ``duplicate`` is
    ``True`` when an identical conversion for the same click had already been
    recorded (the call is idempotent).
    """

    recorded: bool
    duplicate: bool

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> RecordedConversion:
        return cls(
            recorded=bool(data.get("recorded", False)),
            duplicate=bool(data.get("duplicate", False)),
        )


@dataclass(frozen=True, slots=True)
class BatchLinkResult:
    """Outcome of one link in a ``links.create_batch`` call.

    ``index`` is the position of the input in the request array. ``ok`` is
    ``True`` on success — then ``code`` is populated; on failure ``ok`` is
    ``False`` and ``error`` carries the reason.
    """

    index: int
    ok: bool
    code: str | None = None
    error: str | None = None

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> BatchLinkResult:
        return cls(
            index=int(data["index"]),
            ok=bool(data["ok"]),
            code=_opt_str(data.get("code")),
            error=_opt_str(data.get("error")),
        )


@dataclass(frozen=True, slots=True)
class BatchCreateLinksResult:
    """Aggregate result of ``links.create_batch`` (``POST /v1/links/batch``)."""

    created: int
    total: int
    results: tuple[BatchLinkResult, ...]

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> BatchCreateLinksResult:
        return cls(
            created=int(data.get("created", 0)),
            total=int(data.get("total", 0)),
            results=tuple(BatchLinkResult.from_dict(r) for r in data.get("results", []) or []),
        )


WebhookPayloadFormat = Literal["json", "slack"]
"""Delivery payload encoding for a webhook endpoint."""


@dataclass(frozen=True, slots=True)
class Webhook:
    """A webhook endpoint registered to the project.

    Mirrors the server-side ``WebhookEndpointResponse`` shape verbatim.
    """

    id: str
    project_id: str
    name: str
    url: str
    events: tuple[str, ...]
    is_active: bool
    payload_format: str
    created_at: int
    updated_at: int
    last_delivery_at: int | None = None
    last_success_at: int | None = None
    last_failure_at: int | None = None

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> Webhook:
        """Build a ``Webhook`` from the raw JSON dict the API returns."""
        return cls(
            id=str(data["id"]),
            project_id=str(data["project_id"]),
            name=str(data["name"]),
            url=str(data["url"]),
            events=tuple(str(e) for e in data.get("events", []) or []),
            is_active=bool(data["is_active"]),
            payload_format=str(data["payload_format"]),
            created_at=int(data["created_at"]),
            updated_at=int(data["updated_at"]),
            last_delivery_at=_opt_int(data.get("last_delivery_at")),
            last_success_at=_opt_int(data.get("last_success_at")),
            last_failure_at=_opt_int(data.get("last_failure_at")),
        )


@dataclass(frozen=True, slots=True)
class CreatedWebhook:
    """Result of creating a webhook endpoint.

    The ``signing_secret`` (``whsec_…``) is returned **once** — store it now;
    it is never shown again.
    """

    endpoint: Webhook
    signing_secret: str

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> CreatedWebhook:
        return cls(
            endpoint=Webhook.from_dict(data["endpoint"]),
            signing_secret=str(data["signing_secret"]),
        )


@dataclass(frozen=True, slots=True)
class ListWebhooksResult:
    """Webhook endpoints + supported event types from ``GET /v1/projects/me/webhooks``."""

    endpoints: tuple[Webhook, ...]
    event_types: tuple[str, ...]

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> ListWebhooksResult:
        return cls(
            endpoints=tuple(Webhook.from_dict(item) for item in data.get("endpoints", []) or []),
            event_types=tuple(str(e) for e in data.get("event_types", []) or []),
        )


@dataclass(frozen=True, slots=True)
class CreateWebhookInput:
    """Request body for ``POST /v1/projects/me/webhooks``.

    ``name``, ``url``, and ``events`` are required. ``is_active`` defaults to
    ``True`` server-side; ``payload_format`` defaults to ``json`` (or ``slack``
    for Slack URLs). Both optional fields are omitted from the payload unless
    explicitly set.
    """

    name: str
    url: str
    events: tuple[str, ...] | list[str]
    is_active: bool | None = None
    payload_format: WebhookPayloadFormat | None = None

    def to_payload(self) -> dict[str, Any]:
        """Render as the JSON dict the API expects.

        Unset optional fields (``is_active``, ``payload_format`` kept at their
        ``None`` default) are omitted so the server applies its defaults.
        """
        payload: dict[str, Any] = {
            "name": self.name,
            "url": self.url,
            "events": list(self.events),
        }
        if self.is_active is not None:
            payload["is_active"] = self.is_active
        if self.payload_format is not None:
            payload["payload_format"] = self.payload_format
        return payload


@dataclass(frozen=True, slots=True)
class QrOptions:
    """Optional knobs accepted by every QR endpoint.

    Pass ``refresh=True`` to bust the QR cache; the server treats that the
    same as ``refresh="1"``. Pass any other string to use that string as the
    cache-busting token.
    """

    size: int | None = None
    margin: int | None = None
    ecc: EccLevel | None = None
    domain: str | None = None
    refresh: str | bool | None = None

    def to_query_params(self) -> dict[str, str]:
        """Render the options as a flat ``{str: str}`` query-string dict."""
        params: dict[str, str] = {}
        if self.size is not None:
            params["size"] = str(self.size)
        if self.margin is not None:
            params["margin"] = str(self.margin)
        if self.ecc is not None:
            params["ecc"] = self.ecc
        if self.domain is not None:
            params["domain"] = self.domain
        if self.refresh is not None:
            params["refresh"] = "1" if self.refresh is True else str(self.refresh)
        return params


@dataclass(frozen=True, slots=True)
class CreateLinkInput:
    """Request body for ``POST /v1/links``.

    Only ``target_url`` is required. ``code`` requires a verified
    ``domain_hostname``. SEO fields override the defaults the server
    auto-derives from the destination page.
    """

    target_url: str
    domain_hostname: str | None = None
    code: str | None = None
    expires_at: int | None = None
    seo_title: str | None = None
    seo_description: str | None = None
    seo_image_url: str | None = None
    seo_canonical_url: str | None = None
    seo_noindex: bool | None = None
    password: str | None = None
    max_clicks: int | None = None
    track_conversions: bool | None = None
    routing_rules: tuple[RoutingRule, ...] | list[RoutingRule] | None = None
    ab_variants: tuple[AbVariant, ...] | list[AbVariant] | None = None

    def to_payload(self) -> dict[str, Any]:
        """Render as the JSON dict the API expects.

        ``None`` SEO fields are sent through verbatim (they explicitly clear
        the corresponding field server-side). Unset fields — i.e. those
        kept at the dataclass default of ``None`` for ``domain_hostname``,
        ``code``, ``expires_at`` — are omitted from the payload. The Smart
        Link fields (``password``, ``max_clicks``, ``track_conversions``,
        ``routing_rules``, ``ab_variants``) are likewise omitted unless set.
        """
        payload: dict[str, Any] = {"target_url": self.target_url}
        if self.domain_hostname is not None:
            payload["domain_hostname"] = self.domain_hostname
        if self.code is not None:
            payload["code"] = self.code
        if self.expires_at is not None:
            payload["expires_at"] = self.expires_at
        # SEO fields: include only when explicitly set. Mirrors the TS
        # CreateLinkInput shape — callers who want to send a ``null`` to
        # clear them can do so via ``UpdateLinkInput``.
        if self.seo_title is not None:
            payload["seo_title"] = self.seo_title
        if self.seo_description is not None:
            payload["seo_description"] = self.seo_description
        if self.seo_image_url is not None:
            payload["seo_image_url"] = self.seo_image_url
        if self.seo_canonical_url is not None:
            payload["seo_canonical_url"] = self.seo_canonical_url
        if self.seo_noindex is not None:
            payload["seo_noindex"] = self.seo_noindex
        # Smart Link fields: include only when explicitly set.
        if self.password is not None:
            payload["password"] = self.password
        if self.max_clicks is not None:
            payload["max_clicks"] = self.max_clicks
        if self.track_conversions is not None:
            payload["track_conversions"] = self.track_conversions
        if self.routing_rules is not None:
            payload["routing_rules"] = [r.to_payload() for r in self.routing_rules]
        if self.ab_variants is not None:
            payload["ab_variants"] = [v.to_payload() for v in self.ab_variants]
        return payload


@dataclass(frozen=True, slots=True)
class UpdateLinkInput:
    """Request body for ``PATCH /v1/links/:code``.

    Every field defaults to :data:`UNSET`. Pass a value to set the field.
    Pass ``None`` to clear the field on the server (only the SEO fields and
    ``expires_at`` accept ``null``).

    Calling ``to_payload()`` on an empty instance raises :class:`ReroutError`
    with ``code='bad_request'`` so we never hit the API with an empty
    PATCH body.
    """

    target_url: Maybe[str] = field(default=UNSET)
    expires_at: Maybe[int | None] = field(default=UNSET)
    is_active: Maybe[bool] = field(default=UNSET)
    seo_title: Maybe[str | None] = field(default=UNSET)
    seo_description: Maybe[str | None] = field(default=UNSET)
    seo_image_url: Maybe[str | None] = field(default=UNSET)
    seo_canonical_url: Maybe[str | None] = field(default=UNSET)
    seo_noindex: Maybe[bool] = field(default=UNSET)
    password: Maybe[str | None] = field(default=UNSET)
    max_clicks: Maybe[int | None] = field(default=UNSET)
    track_conversions: Maybe[bool] = field(default=UNSET)
    routing_rules: Maybe[tuple[RoutingRule, ...] | list[RoutingRule]] = field(default=UNSET)
    ab_variants: Maybe[tuple[AbVariant, ...] | list[AbVariant]] = field(default=UNSET)

    def to_payload(self) -> dict[str, Any]:
        """Render as the JSON dict the API expects.

        Fields that are :data:`UNSET` are omitted. Fields set to ``None`` are
        included as JSON ``null`` so the server clears them (``password`` and
        ``max_clicks`` accept ``null``). ``routing_rules`` and ``ab_variants``
        are a full replacement of the link's Smart Link config.
        """
        payload: dict[str, Any] = {}
        if not isinstance(self.target_url, _UnsetType):
            payload["target_url"] = self.target_url
        if not isinstance(self.expires_at, _UnsetType):
            payload["expires_at"] = self.expires_at
        if not isinstance(self.is_active, _UnsetType):
            payload["is_active"] = self.is_active
        if not isinstance(self.seo_title, _UnsetType):
            payload["seo_title"] = self.seo_title
        if not isinstance(self.seo_description, _UnsetType):
            payload["seo_description"] = self.seo_description
        if not isinstance(self.seo_image_url, _UnsetType):
            payload["seo_image_url"] = self.seo_image_url
        if not isinstance(self.seo_canonical_url, _UnsetType):
            payload["seo_canonical_url"] = self.seo_canonical_url
        if not isinstance(self.seo_noindex, _UnsetType):
            payload["seo_noindex"] = self.seo_noindex
        if not isinstance(self.password, _UnsetType):
            payload["password"] = self.password
        if not isinstance(self.max_clicks, _UnsetType):
            payload["max_clicks"] = self.max_clicks
        if not isinstance(self.track_conversions, _UnsetType):
            payload["track_conversions"] = self.track_conversions
        if not isinstance(self.routing_rules, _UnsetType):
            payload["routing_rules"] = [r.to_payload() for r in self.routing_rules]
        if not isinstance(self.ab_variants, _UnsetType):
            payload["ab_variants"] = [v.to_payload() for v in self.ab_variants]
        return payload

    @property
    def is_empty(self) -> bool:
        """True when no field has been set — sending this would be a no-op."""
        return len(self.to_payload()) == 0


def _opt_str(value: Any) -> str | None:
    return None if value is None else str(value)


def _opt_int(value: Any) -> int | None:
    return None if value is None else int(value)
