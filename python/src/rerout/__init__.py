"""Official Python SDK for the Rerout branded-link API.

Branded link infrastructure on Cloudflare. Create short links, render QR
codes, read analytics, and verify webhook signatures.

Example:
    >>> from rerout import Rerout, CreateLinkInput
    >>> rerout = Rerout(api_key="rrk_...")
    >>> link = rerout.links.create(
    ...     CreateLinkInput(target_url="https://example.com/sale")
    ... )
    >>> print(link.short_url)

See https://rerout.co and
https://github.com/ModestNerds-Co/rerout-sdks for more details.
"""

from __future__ import annotations

from ._client import DEFAULT_BASE_URL, Links, Project, Qr, Rerout
from ._errors import ReroutError
from ._models import (
    UNSET,
    CreateLinkInput,
    DailyClicksPoint,
    EccLevel,
    Link,
    LinkStats,
    ListLinksResult,
    ProjectInfo,
    ProjectStats,
    QrOptions,
    StatsBreakdown,
    Tag,
    UpdateLinkInput,
)
from ._webhooks import (
    DEFAULT_SIGNATURE_TOLERANCE_SECONDS,
    verify_rerout_signature,
)

__version__ = "0.2.0"

__all__ = [
    "DEFAULT_BASE_URL",
    "DEFAULT_SIGNATURE_TOLERANCE_SECONDS",
    "UNSET",
    "CreateLinkInput",
    "DailyClicksPoint",
    "EccLevel",
    "Link",
    "LinkStats",
    "Links",
    "ListLinksResult",
    "Project",
    "ProjectInfo",
    "ProjectStats",
    "Qr",
    "QrOptions",
    "Rerout",
    "ReroutError",
    "StatsBreakdown",
    "Tag",
    "UpdateLinkInput",
    "__version__",
    "verify_rerout_signature",
]
