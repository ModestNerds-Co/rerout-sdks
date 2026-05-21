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
from typing import Any, Final, Literal


class _UnsetType:
    """Sentinel singleton meaning "this field was not provided".

    The :class:`UpdateLinkInput` dataclass uses this to differentiate between
    a caller passing ``None`` (clear the field on the server) and not passing
    a value at all (leave the field untouched).
    """

    _instance: "_UnsetType | None" = None

    def __new__(cls) -> "_UnsetType":
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __repr__(self) -> str:
        return "UNSET"

    def __bool__(self) -> bool:
        return False


UNSET: Final[_UnsetType] = _UnsetType()
"""Singleton sentinel used as the default for optional update-input fields."""


# Type alias for "value, or omitted".
type Maybe[T] = T | _UnsetType


EccLevel = Literal["L", "M", "Q", "H"]


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
    domain_hostname: str | None = None
    expires_at: int | None = None
    seo_title: str | None = None
    seo_description: str | None = None
    seo_image_url: str | None = None
    seo_canonical_url: str | None = None
    seo_updated_at: int | None = None

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "Link":
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
            domain_hostname=_opt_str(data.get("domain_hostname")),
            expires_at=_opt_int(data.get("expires_at")),
            seo_title=_opt_str(data.get("seo_title")),
            seo_description=_opt_str(data.get("seo_description")),
            seo_image_url=_opt_str(data.get("seo_image_url")),
            seo_canonical_url=_opt_str(data.get("seo_canonical_url")),
            seo_updated_at=_opt_int(data.get("seo_updated_at")),
        )


@dataclass(frozen=True, slots=True)
class ListLinksResult:
    """Paginated list of links returned by ``GET /v1/links``."""

    links: tuple[Link, ...]
    next_cursor: int | None

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "ListLinksResult":
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
    def from_dict(cls, data: dict[str, Any]) -> "StatsBreakdown":
        return cls(value=str(data["value"]), clicks=int(data["clicks"]))


@dataclass(frozen=True, slots=True)
class DailyClicksPoint:
    """One day of click + QR scan activity."""

    day: int
    clicks: int
    qr_scans: int

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "DailyClicksPoint":
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
    def from_dict(cls, data: dict[str, Any]) -> "ProjectStats":
        return cls(
            days=int(data["days"]),
            total_clicks=int(data["total_clicks"]),
            qr_scans=int(data["qr_scans"]),
            daily=tuple(DailyClicksPoint.from_dict(d) for d in data.get("daily", []) or []),
            countries=tuple(
                StatsBreakdown.from_dict(d) for d in data.get("countries", []) or []
            ),
            referrers=tuple(
                StatsBreakdown.from_dict(d) for d in data.get("referrers", []) or []
            ),
            devices=tuple(StatsBreakdown.from_dict(d) for d in data.get("devices", []) or []),
            browsers=tuple(
                StatsBreakdown.from_dict(d) for d in data.get("browsers", []) or []
            ),
            top_codes=tuple(
                StatsBreakdown.from_dict(d) for d in data.get("top_codes", []) or []
            ),
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
    def from_dict(cls, data: dict[str, Any]) -> "LinkStats":
        return cls(
            code=str(data["code"]),
            days=int(data["days"]),
            total_clicks=int(data["total_clicks"]),
            qr_scans=int(data["qr_scans"]),
            countries=tuple(
                StatsBreakdown.from_dict(d) for d in data.get("countries", []) or []
            ),
            referrers=tuple(
                StatsBreakdown.from_dict(d) for d in data.get("referrers", []) or []
            ),
        )


@dataclass(frozen=True, slots=True)
class ProjectInfo:
    """Identifying info for the project that owns the current API key."""

    id: str
    name: str
    slug: str

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "ProjectInfo":
        return cls(
            id=str(data["id"]),
            name=str(data["name"]),
            slug=str(data["slug"]),
        )


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

    def to_payload(self) -> dict[str, Any]:
        """Render as the JSON dict the API expects.

        ``None`` SEO fields are sent through verbatim (they explicitly clear
        the corresponding field server-side). Unset fields — i.e. those
        kept at the dataclass default of ``None`` for ``domain_hostname``,
        ``code``, ``expires_at`` — are omitted from the payload.
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

    def to_payload(self) -> dict[str, Any]:
        """Render as the JSON dict the API expects.

        Fields that are :data:`UNSET` are omitted. Fields set to ``None`` are
        included as JSON ``null`` so the server clears them.
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
        return payload

    @property
    def is_empty(self) -> bool:
        """True when no field has been set — sending this would be a no-op."""
        return len(self.to_payload()) == 0


def _opt_str(value: Any) -> str | None:
    return None if value is None else str(value)


def _opt_int(value: Any) -> int | None:
    return None if value is None else int(value)
