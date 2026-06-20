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

from ._client import (
    DEFAULT_BASE_URL,
    Conversions,
    Links,
    Project,
    Qr,
    Rerout,
    Tags,
    Webhooks,
)
from ._errors import ReroutError
from ._models import (
    UNSET,
    AbVariant,
    BatchCreateLinksResult,
    BatchLinkResult,
    ConditionOp,
    ConditionType,
    CreatedWebhook,
    CreateLinkInput,
    CreateTagInput,
    CreateWebhookInput,
    DailyClicksPoint,
    EccLevel,
    Link,
    LinkStats,
    ListLinksResult,
    ListTagsResult,
    ListWebhooksResult,
    ProjectInfo,
    ProjectStats,
    QrOptions,
    RecordedConversion,
    RoutingRule,
    StatsBreakdown,
    Tag,
    TagSummary,
    UpdateLinkInput,
    UpdateTagInput,
    Webhook,
    WebhookPayloadFormat,
)
from ._webhooks import (
    DEFAULT_SIGNATURE_TOLERANCE_SECONDS,
    verify_rerout_signature,
)

__version__ = "0.5.0"

__all__ = [
    "DEFAULT_BASE_URL",
    "DEFAULT_SIGNATURE_TOLERANCE_SECONDS",
    "UNSET",
    "AbVariant",
    "BatchCreateLinksResult",
    "BatchLinkResult",
    "ConditionOp",
    "ConditionType",
    "Conversions",
    "CreateLinkInput",
    "CreateTagInput",
    "CreateWebhookInput",
    "CreatedWebhook",
    "DailyClicksPoint",
    "EccLevel",
    "Link",
    "LinkStats",
    "Links",
    "ListLinksResult",
    "ListTagsResult",
    "ListWebhooksResult",
    "Project",
    "ProjectInfo",
    "ProjectStats",
    "Qr",
    "QrOptions",
    "RecordedConversion",
    "Rerout",
    "ReroutError",
    "RoutingRule",
    "StatsBreakdown",
    "Tag",
    "TagSummary",
    "Tags",
    "UpdateLinkInput",
    "UpdateTagInput",
    "Webhook",
    "WebhookPayloadFormat",
    "Webhooks",
    "__version__",
    "verify_rerout_signature",
]
