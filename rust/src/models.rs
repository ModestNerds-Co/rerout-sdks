//! Public data types returned by the Rerout API.
//!
//! Field names match the JSON envelope verbatim so serde can deserialize
//! without rename rules at the call site.

use serde::{Deserialize, Serialize};

// ─── Tag ────────────────────────────────────────────────────────────────────

/// A label attached to a [`Link`]. Read-only — tag writes are ignored for
/// API-key clients.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Tag {
    /// Tag ID.
    pub id: String,
    /// Display name.
    pub name: String,
    /// Hex color (e.g. `#3b82f6`).
    pub color: String,
}

// ─── Smart Links ──────────────────────────────────────────────────────────

/// A Smart-Links conditional redirect rule.
///
/// When an inbound request matches the condition, the link routes to
/// [`RoutingRule::target_url`] instead of the link's default destination.
///
/// `condition_type` is `"country"` or `"device"`; `condition_op` is `"is"`,
/// `"is_not"`, or `"in"`. For the `"in"` operator, `condition_value` is a
/// comma-separated list.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct RoutingRule {
    /// What the rule matches on — `"country"` or `"device"`.
    pub condition_type: String,
    /// Comparison operator — `"is"`, `"is_not"`, or `"in"`.
    pub condition_op: String,
    /// Value(s) to compare against. Comma-separated for the `"in"` operator.
    pub condition_value: String,
    /// Destination URL used when the condition matches.
    pub target_url: String,
}

/// A weighted Smart-Links A/B testing destination as returned by the API.
///
/// Inbound traffic is split across variants in proportion to their `weight`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct AbVariant {
    /// Server-assigned variant ID.
    pub id: i64,
    /// Destination URL for this variant.
    pub target_url: String,
    /// Relative traffic weight.
    #[serde(default)]
    pub weight: i32,
}

/// A weighted A/B variant on [`CreateLinkInput`] / [`UpdateLinkInput`].
///
/// `weight` is optional — the server applies an even split when it is `None`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct CreateAbVariantInput {
    /// Destination URL for this variant.
    pub target_url: String,
    /// Relative traffic weight. Omitted when `None`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub weight: Option<i32>,
}

impl CreateAbVariantInput {
    /// Build a variant with just a destination URL — even split.
    pub fn new(target_url: impl Into<String>) -> Self {
        Self {
            target_url: target_url.into(),
            weight: None,
        }
    }

    /// Builder — set the relative traffic weight.
    #[must_use]
    pub fn with_weight(mut self, weight: i32) -> Self {
        self.weight = Some(weight);
        self
    }
}

// ─── Link ───────────────────────────────────────────────────────────────────

/// A single short link as returned by the Rerout API.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Link {
    /// Short link path code.
    pub code: String,
    /// Fully-qualified short URL — `https://{host}/{code}`.
    pub short_url: String,
    /// Verified custom domain hosting the link, when one is bound.
    pub domain_hostname: Option<String>,
    /// Destination URL the redirect resolves to.
    pub target_url: String,
    /// Project that owns the link.
    pub project_id: String,
    /// Unix seconds when the link expires. `None` for permanent links.
    pub expires_at: Option<i64>,
    /// Whether the link is currently active.
    pub is_active: bool,
    /// Override social preview title.
    pub seo_title: Option<String>,
    /// Override social preview description.
    pub seo_description: Option<String>,
    /// Override social preview image URL.
    pub seo_image_url: Option<String>,
    /// Override preview canonical URL.
    pub seo_canonical_url: Option<String>,
    /// Whether the preview HTML is marked noindex.
    #[serde(default)]
    pub seo_noindex: bool,
    /// Unix seconds — last SEO mutation.
    pub seo_updated_at: Option<i64>,
    /// Unix seconds — link creation time.
    pub created_at: i64,
    /// Unix seconds — last mutation.
    pub updated_at: i64,
    /// Read-only tags attached to the link. Empty on create; populated on
    /// get, list, and update. A missing field deserializes to an empty `Vec`.
    #[serde(default)]
    pub tags: Vec<Tag>,
    /// Whether the link requires a password before redirecting. The password
    /// itself is never returned.
    #[serde(default)]
    pub password_protected: bool,
    /// Click cap after which the link stops redirecting. `None` when uncapped.
    pub max_clicks: Option<i64>,
    /// Number of clicks recorded against the link so far.
    #[serde(default)]
    pub click_count: i64,
    /// Whether conversion tracking is enabled for this link.
    #[serde(default)]
    pub track_conversions: bool,
    /// Conditional redirect rules, evaluated in order. A missing field
    /// deserializes to an empty `Vec`.
    #[serde(default)]
    pub routing_rules: Vec<RoutingRule>,
    /// Weighted A/B testing destinations. A missing field deserializes to an
    /// empty `Vec`.
    #[serde(default)]
    pub ab_variants: Vec<AbVariant>,
}

// ─── Inputs ─────────────────────────────────────────────────────────────────

/// Body for `POST /v1/links`.
///
/// `target_url` is required. Every other field is optional and only sent when
/// `Some`. Use [`CreateLinkInput::new`] for the minimal happy path.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, Default)]
pub struct CreateLinkInput {
    /// Absolute `https://` destination URL.
    pub target_url: String,
    /// Verified custom domain to host this link on.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub domain_hostname: Option<String>,
    /// Custom path. Only valid when `domain_hostname` is supplied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Unix seconds — expiration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expires_at: Option<i64>,
    /// Override social preview title.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_title: Option<String>,
    /// Override social preview description.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_description: Option<String>,
    /// Absolute social preview image URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_image_url: Option<String>,
    /// Canonical URL for the preview HTML.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_canonical_url: Option<String>,
    /// Whether the preview HTML is marked noindex.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_noindex: Option<bool>,
    /// Password required before the link redirects.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub password: Option<String>,
    /// Click cap after which the link stops redirecting.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_clicks: Option<i64>,
    /// Enable conversion tracking for this link.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub track_conversions: Option<bool>,
    /// Conditional redirect rules, evaluated in order.
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub routing_rules: Vec<RoutingRule>,
    /// Weighted A/B testing destinations.
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub ab_variants: Vec<CreateAbVariantInput>,
}

impl CreateLinkInput {
    /// Build the minimal input: just a destination URL.
    pub fn new(target_url: impl Into<String>) -> Self {
        Self {
            target_url: target_url.into(),
            ..Self::default()
        }
    }

    /// Builder — set the verified custom domain to host this link on.
    #[must_use]
    pub fn with_domain_hostname(mut self, domain_hostname: impl Into<String>) -> Self {
        self.domain_hostname = Some(domain_hostname.into());
        self
    }

    /// Builder — set the custom code (path).
    #[must_use]
    pub fn with_code(mut self, code: impl Into<String>) -> Self {
        self.code = Some(code.into());
        self
    }

    /// Builder — set the expiration in unix seconds.
    #[must_use]
    pub fn with_expires_at(mut self, expires_at: i64) -> Self {
        self.expires_at = Some(expires_at);
        self
    }

    /// Builder — set the SEO title override.
    #[must_use]
    pub fn with_seo_title(mut self, seo_title: impl Into<String>) -> Self {
        self.seo_title = Some(seo_title.into());
        self
    }

    /// Builder — set the SEO description override.
    #[must_use]
    pub fn with_seo_description(mut self, seo_description: impl Into<String>) -> Self {
        self.seo_description = Some(seo_description.into());
        self
    }

    /// Builder — set the SEO image URL override.
    #[must_use]
    pub fn with_seo_image_url(mut self, seo_image_url: impl Into<String>) -> Self {
        self.seo_image_url = Some(seo_image_url.into());
        self
    }

    /// Builder — set the SEO canonical URL override.
    #[must_use]
    pub fn with_seo_canonical_url(mut self, seo_canonical_url: impl Into<String>) -> Self {
        self.seo_canonical_url = Some(seo_canonical_url.into());
        self
    }

    /// Builder — toggle the noindex flag.
    #[must_use]
    pub fn with_seo_noindex(mut self, seo_noindex: bool) -> Self {
        self.seo_noindex = Some(seo_noindex);
        self
    }

    /// Builder — require a password before redirecting.
    #[must_use]
    pub fn with_password(mut self, password: impl Into<String>) -> Self {
        self.password = Some(password.into());
        self
    }

    /// Builder — cap the number of redirects.
    #[must_use]
    pub fn with_max_clicks(mut self, max_clicks: i64) -> Self {
        self.max_clicks = Some(max_clicks);
        self
    }

    /// Builder — enable conversion tracking.
    #[must_use]
    pub fn with_track_conversions(mut self, track_conversions: bool) -> Self {
        self.track_conversions = Some(track_conversions);
        self
    }

    /// Builder — set the conditional redirect rules.
    #[must_use]
    pub fn with_routing_rules(mut self, routing_rules: Vec<RoutingRule>) -> Self {
        self.routing_rules = routing_rules;
        self
    }

    /// Builder — set the weighted A/B testing variants.
    #[must_use]
    pub fn with_ab_variants(mut self, ab_variants: Vec<CreateAbVariantInput>) -> Self {
        self.ab_variants = ab_variants;
        self
    }
}

/// Body for `PATCH /v1/links/:code`.
///
/// Every field uses the `Option<Option<T>>` pattern:
/// - The outer `None` means "leave the field alone" — serde skips serializing.
/// - `Some(None)` serializes as JSON `null` — explicit "clear the value".
/// - `Some(Some(value))` serializes as `value` — the normal set case.
///
/// Builder methods cover the common cases — `set_*` for "Some(Some(value))"
/// and `clear_*` for "Some(None)".
///
/// The empty payload is rejected client-side ([`UpdateLinkInput::is_empty`])
/// and the [`crate::Links::update`] call returns a `bad_request` config error
/// without hitting the server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct UpdateLinkInput {
    /// New destination URL.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub target_url: Option<Option<String>>,
    /// New expiration (unix seconds) or explicit null to clear.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expires_at: Option<Option<i64>>,
    /// Toggle whether the link is active.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_active: Option<Option<bool>>,
    /// New SEO title, or explicit null to clear.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_title: Option<Option<String>>,
    /// New SEO description, or explicit null to clear.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_description: Option<Option<String>>,
    /// New SEO image URL, or explicit null to clear.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_image_url: Option<Option<String>>,
    /// New SEO canonical URL, or explicit null to clear.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_canonical_url: Option<Option<String>>,
    /// Toggle the noindex flag.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seo_noindex: Option<Option<bool>>,
    /// New password, or explicit null to remove password protection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub password: Option<Option<String>>,
    /// New click cap, or explicit null to remove the cap.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_clicks: Option<Option<i64>>,
    /// Toggle conversion tracking.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub track_conversions: Option<Option<bool>>,
    /// Full replacement of the link's routing rules. `Some(vec)` replaces them
    /// (an empty `Vec` clears all rules); `None` leaves them untouched.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub routing_rules: Option<Vec<RoutingRule>>,
    /// Full replacement of the link's A/B variants. `Some(vec)` replaces them
    /// (an empty `Vec` clears all variants); `None` leaves them untouched.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ab_variants: Option<Vec<CreateAbVariantInput>>,
}

impl UpdateLinkInput {
    /// Create an empty update — populate via the `set_*` / `clear_*` builders.
    pub fn new() -> Self {
        Self::default()
    }

    /// `true` when no field has been set — calling `update` with this would
    /// be rejected client-side.
    pub fn is_empty(&self) -> bool {
        self.target_url.is_none()
            && self.expires_at.is_none()
            && self.is_active.is_none()
            && self.seo_title.is_none()
            && self.seo_description.is_none()
            && self.seo_image_url.is_none()
            && self.seo_canonical_url.is_none()
            && self.seo_noindex.is_none()
            && self.password.is_none()
            && self.max_clicks.is_none()
            && self.track_conversions.is_none()
            && self.routing_rules.is_none()
            && self.ab_variants.is_none()
    }

    /// Set a new `target_url`.
    #[must_use]
    pub fn set_target_url(mut self, target_url: impl Into<String>) -> Self {
        self.target_url = Some(Some(target_url.into()));
        self
    }

    /// Set a new `expires_at`.
    #[must_use]
    pub fn set_expires_at(mut self, expires_at: i64) -> Self {
        self.expires_at = Some(Some(expires_at));
        self
    }

    /// Clear an existing expiration — sends `expires_at: null`.
    #[must_use]
    pub fn clear_expires_at(mut self) -> Self {
        self.expires_at = Some(None);
        self
    }

    /// Toggle whether the link is active.
    #[must_use]
    pub fn set_is_active(mut self, is_active: bool) -> Self {
        self.is_active = Some(Some(is_active));
        self
    }

    /// Set a new SEO title.
    #[must_use]
    pub fn set_seo_title(mut self, seo_title: impl Into<String>) -> Self {
        self.seo_title = Some(Some(seo_title.into()));
        self
    }

    /// Clear an existing SEO title — sends `seo_title: null`.
    #[must_use]
    pub fn clear_seo_title(mut self) -> Self {
        self.seo_title = Some(None);
        self
    }

    /// Set a new SEO description.
    #[must_use]
    pub fn set_seo_description(mut self, seo_description: impl Into<String>) -> Self {
        self.seo_description = Some(Some(seo_description.into()));
        self
    }

    /// Clear an existing SEO description.
    #[must_use]
    pub fn clear_seo_description(mut self) -> Self {
        self.seo_description = Some(None);
        self
    }

    /// Set a new SEO image URL.
    #[must_use]
    pub fn set_seo_image_url(mut self, seo_image_url: impl Into<String>) -> Self {
        self.seo_image_url = Some(Some(seo_image_url.into()));
        self
    }

    /// Clear an existing SEO image URL.
    #[must_use]
    pub fn clear_seo_image_url(mut self) -> Self {
        self.seo_image_url = Some(None);
        self
    }

    /// Set a new SEO canonical URL.
    #[must_use]
    pub fn set_seo_canonical_url(mut self, seo_canonical_url: impl Into<String>) -> Self {
        self.seo_canonical_url = Some(Some(seo_canonical_url.into()));
        self
    }

    /// Clear an existing SEO canonical URL.
    #[must_use]
    pub fn clear_seo_canonical_url(mut self) -> Self {
        self.seo_canonical_url = Some(None);
        self
    }

    /// Toggle the noindex flag.
    #[must_use]
    pub fn set_seo_noindex(mut self, seo_noindex: bool) -> Self {
        self.seo_noindex = Some(Some(seo_noindex));
        self
    }

    /// Set a new password.
    #[must_use]
    pub fn set_password(mut self, password: impl Into<String>) -> Self {
        self.password = Some(Some(password.into()));
        self
    }

    /// Remove password protection — sends `password: null`.
    #[must_use]
    pub fn clear_password(mut self) -> Self {
        self.password = Some(None);
        self
    }

    /// Set a new click cap.
    #[must_use]
    pub fn set_max_clicks(mut self, max_clicks: i64) -> Self {
        self.max_clicks = Some(Some(max_clicks));
        self
    }

    /// Remove the click cap — sends `max_clicks: null`.
    #[must_use]
    pub fn clear_max_clicks(mut self) -> Self {
        self.max_clicks = Some(None);
        self
    }

    /// Toggle conversion tracking.
    #[must_use]
    pub fn set_track_conversions(mut self, track_conversions: bool) -> Self {
        self.track_conversions = Some(Some(track_conversions));
        self
    }

    /// Replace the link's routing rules. An empty `Vec` clears all rules.
    #[must_use]
    pub fn set_routing_rules(mut self, routing_rules: Vec<RoutingRule>) -> Self {
        self.routing_rules = Some(routing_rules);
        self
    }

    /// Replace the link's A/B variants. An empty `Vec` clears all variants.
    #[must_use]
    pub fn set_ab_variants(mut self, ab_variants: Vec<CreateAbVariantInput>) -> Self {
        self.ab_variants = Some(ab_variants);
        self
    }
}

// ─── List ───────────────────────────────────────────────────────────────────

/// A page of [`Link`] results from `GET /v1/links`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ListLinksResult {
    /// Links on this page, newest first.
    pub links: Vec<Link>,
    /// Cursor for the next page, or `None` when this is the last page.
    pub next_cursor: Option<i64>,
}

impl ListLinksResult {
    /// Whether more pages remain.
    pub fn has_more(&self) -> bool {
        self.next_cursor.is_some()
    }
}

// ─── Stats ──────────────────────────────────────────────────────────────────

/// A single bucket in an analytics breakdown — one country, one device, etc.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct StatsBreakdown {
    /// Bucket label (e.g. country code, device class, browser name).
    #[serde(default)]
    pub value: String,
    /// Click count for this bucket.
    #[serde(default)]
    pub clicks: i64,
}

/// One point in a daily clicks time series.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct DailyClicksPoint {
    /// Day bucket — unix seconds at 00:00 UTC.
    pub day: i64,
    /// Total clicks (link + QR) that day.
    #[serde(default)]
    pub clicks: i64,
    /// Subset of `clicks` attributed to a QR scan.
    #[serde(default)]
    pub qr_scans: i64,
}

/// Analytics for a single short link across the requested window.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct LinkStats {
    /// The short code these stats belong to.
    #[serde(default)]
    pub code: String,
    /// Window size in days.
    #[serde(default)]
    pub days: i64,
    /// Total clicks in the window.
    #[serde(default)]
    pub total_clicks: i64,
    /// Subset attributed to QR scans.
    #[serde(default)]
    pub qr_scans: i64,
    /// Top countries by click count.
    #[serde(default)]
    pub countries: Vec<StatsBreakdown>,
    /// Top referrers by click count.
    #[serde(default)]
    pub referrers: Vec<StatsBreakdown>,
}

/// Aggregate analytics for a project across the requested window.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ProjectStats {
    /// Window size in days.
    #[serde(default)]
    pub days: i64,
    /// Total clicks recorded in the window.
    #[serde(default)]
    pub total_clicks: i64,
    /// Subset attributed to QR scans.
    #[serde(default)]
    pub qr_scans: i64,
    /// One point per day across the window.
    #[serde(default)]
    pub daily: Vec<DailyClicksPoint>,
    /// Top countries.
    #[serde(default)]
    pub countries: Vec<StatsBreakdown>,
    /// Top referrers.
    #[serde(default)]
    pub referrers: Vec<StatsBreakdown>,
    /// Click share by device class.
    #[serde(default)]
    pub devices: Vec<StatsBreakdown>,
    /// Click share by browser.
    #[serde(default)]
    pub browsers: Vec<StatsBreakdown>,
    /// Top short codes.
    #[serde(default)]
    pub top_codes: Vec<StatsBreakdown>,
}

/// Response body for `GET /v1/projects/me`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ProjectInfo {
    /// Project ID.
    pub id: String,
    /// Project name.
    pub name: String,
    /// URL-safe slug.
    pub slug: String,
}

/// Response body for `DELETE /v1/links/:code`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct DeleteLinkResult {
    /// Whether the delete succeeded.
    #[serde(default)]
    pub deleted: bool,
}

// ─── Conversions ────────────────────────────────────────────────────────────

/// Body for `POST /v1/conversions`.
///
/// `click_id` and `event_name` are required. `value_cents` and `currency` are
/// optional and only sent when `Some`. Use [`RecordConversionInput::new`] for
/// the minimal happy path.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, Default)]
pub struct RecordConversionInput {
    /// The click this conversion is attributed to.
    pub click_id: String,
    /// Name of the conversion event (e.g. `purchase`, `signup`).
    pub event_name: String,
    /// Monetary value in the smallest currency unit (cents).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub value_cents: Option<i64>,
    /// ISO 4217 currency code (e.g. `USD`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub currency: Option<String>,
}

impl RecordConversionInput {
    /// Build the minimal input: a click id and an event name.
    pub fn new(click_id: impl Into<String>, event_name: impl Into<String>) -> Self {
        Self {
            click_id: click_id.into(),
            event_name: event_name.into(),
            value_cents: None,
            currency: None,
        }
    }

    /// Builder — set the conversion value in cents.
    #[must_use]
    pub fn with_value_cents(mut self, value_cents: i64) -> Self {
        self.value_cents = Some(value_cents);
        self
    }

    /// Builder — set the ISO 4217 currency code.
    #[must_use]
    pub fn with_currency(mut self, currency: impl Into<String>) -> Self {
        self.currency = Some(currency.into());
        self
    }
}

/// Response body for `POST /v1/conversions`.
///
/// The call is idempotent on the `(click_id, event_name)` pair: `recorded`
/// reflects whether a new row was written, and `duplicate` is `true` when the
/// pair was already recorded.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct ConversionResult {
    /// Whether a new conversion row was written.
    #[serde(default)]
    pub recorded: bool,
    /// Whether the event duplicated one already stored.
    #[serde(default)]
    pub duplicate: bool,
}

// ─── Batch ──────────────────────────────────────────────────────────────────

/// One entry in a [`crate::Links::create_batch`] request.
///
/// `target_url` is required; the rest are optional and only sent when `Some`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, Default)]
pub struct BatchLinkInput {
    /// Absolute `https://` destination URL.
    pub target_url: String,
    /// Custom path. Only valid with a verified `domain_hostname`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Unix seconds — expiration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expires_at: Option<i64>,
    /// Verified custom domain to host this link on.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub domain_hostname: Option<String>,
}

impl BatchLinkInput {
    /// Build the minimal entry: just a destination URL.
    pub fn new(target_url: impl Into<String>) -> Self {
        Self {
            target_url: target_url.into(),
            ..Self::default()
        }
    }

    /// Builder — set the custom code (path).
    #[must_use]
    pub fn with_code(mut self, code: impl Into<String>) -> Self {
        self.code = Some(code.into());
        self
    }

    /// Builder — set the expiration in unix seconds.
    #[must_use]
    pub fn with_expires_at(mut self, expires_at: i64) -> Self {
        self.expires_at = Some(expires_at);
        self
    }

    /// Builder — set the verified custom domain.
    #[must_use]
    pub fn with_domain_hostname(mut self, domain_hostname: impl Into<String>) -> Self {
        self.domain_hostname = Some(domain_hostname.into());
        self
    }
}

/// Per-item outcome of a [`crate::Links::create_batch`] request.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct BatchLinkResult {
    /// Zero-based position of the input link.
    pub index: usize,
    /// Whether this link was created successfully.
    pub ok: bool,
    /// New short code, present on success.
    pub code: Option<String>,
    /// Failure reason, present when `ok` is `false`.
    pub error: Option<String>,
}

/// Response body for `POST /v1/links/batch`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct BatchCreateLinksResult {
    /// Number of links created successfully.
    #[serde(default)]
    pub created: usize,
    /// Total number of links in the request.
    #[serde(default)]
    pub total: usize,
    /// Per-item results, in request order.
    #[serde(default)]
    pub results: Vec<BatchLinkResult>,
}

// ─── Webhooks ─────────────────────────────────────────────────────────────────

/// Delivery payload encoding for a webhook endpoint.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum WebhookPayloadFormat {
    /// Standard signed JSON delivery.
    Json,
    /// Slack-compatible incoming-webhook payload.
    Slack,
}

impl WebhookPayloadFormat {
    /// Wire representation (`"json"` or `"slack"`).
    pub fn as_str(self) -> &'static str {
        match self {
            WebhookPayloadFormat::Json => "json",
            WebhookPayloadFormat::Slack => "slack",
        }
    }
}

/// A webhook endpoint registered to the project. Mirrors the server-side
/// `WebhookEndpointResponse` shape.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Webhook {
    /// Endpoint ID (`wh_…`).
    pub id: String,
    /// Project that owns the endpoint.
    pub project_id: String,
    /// Human-readable label.
    pub name: String,
    /// Public `https://` URL that receives signed POST deliveries.
    pub url: String,
    /// Event types the endpoint is subscribed to.
    #[serde(default)]
    pub events: Vec<String>,
    /// Whether the endpoint is currently active.
    pub is_active: bool,
    /// Delivery payload encoding (`json` or `slack`).
    pub payload_format: String,
    /// Unix seconds — endpoint creation time.
    pub created_at: i64,
    /// Unix seconds — last mutation.
    pub updated_at: i64,
    /// Unix seconds — last delivery attempt. `None` if never delivered.
    pub last_delivery_at: Option<i64>,
    /// Unix seconds — last successful delivery. `None` if none succeeded.
    pub last_success_at: Option<i64>,
    /// Unix seconds — last failed delivery. `None` if none failed.
    pub last_failure_at: Option<i64>,
}

/// Body for `POST /v1/projects/me/webhooks`.
///
/// `name`, `url`, and `events` are required. `is_active` and `payload_format`
/// are optional and only sent when `Some`. Use [`CreateWebhookInput::new`] for
/// the minimal happy path.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, Default)]
pub struct CreateWebhookInput {
    /// Human-readable label for the endpoint.
    pub name: String,
    /// Public `https://` URL that receives signed POST deliveries.
    pub url: String,
    /// Event types to subscribe to (e.g. `link.created`). At least one.
    pub events: Vec<String>,
    /// Whether the endpoint starts active. Defaults to `true` server-side.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_active: Option<bool>,
    /// Payload encoding. Defaults to `json` server-side.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payload_format: Option<WebhookPayloadFormat>,
}

impl CreateWebhookInput {
    /// Build the minimal input: a name, a destination URL, and the events to
    /// subscribe to.
    pub fn new(
        name: impl Into<String>,
        url: impl Into<String>,
        events: impl IntoIterator<Item = impl Into<String>>,
    ) -> Self {
        Self {
            name: name.into(),
            url: url.into(),
            events: events.into_iter().map(Into::into).collect(),
            is_active: None,
            payload_format: None,
        }
    }

    /// Builder — set whether the endpoint starts active.
    #[must_use]
    pub fn with_is_active(mut self, is_active: bool) -> Self {
        self.is_active = Some(is_active);
        self
    }

    /// Builder — set the payload encoding.
    #[must_use]
    pub fn with_payload_format(mut self, payload_format: WebhookPayloadFormat) -> Self {
        self.payload_format = Some(payload_format);
        self
    }
}

/// Result of creating a webhook endpoint.
///
/// The `signing_secret` (`whsec_…`) is returned **once** — store it now to
/// verify future deliveries with [`crate::verify_rerout_signature`]; it is
/// never shown again.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct CreatedWebhook {
    /// The newly-created endpoint.
    pub endpoint: Webhook,
    /// One-time signing secret (`whsec_…`).
    pub signing_secret: String,
}

/// Response body for `GET /v1/projects/me/webhooks`.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ListWebhooksResult {
    /// Registered endpoints.
    #[serde(default)]
    pub endpoints: Vec<Webhook>,
    /// Every event type the server can deliver.
    #[serde(default)]
    pub event_types: Vec<String>,
}

/// Response body for `DELETE /v1/projects/me/webhooks/:id`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct DeleteWebhookResult {
    /// Whether the delete succeeded.
    #[serde(default)]
    pub deleted: bool,
}

// ─── QR ─────────────────────────────────────────────────────────────────────

/// Error-correction levels for the QR endpoint.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum QrEcc {
    /// Low — up to 7 % recovery.
    #[serde(rename = "L")]
    L,
    /// Medium — up to 15 % recovery.
    #[serde(rename = "M")]
    M,
    /// Quartile — up to 25 % recovery.
    #[serde(rename = "Q")]
    Q,
    /// High — up to 30 % recovery.
    #[serde(rename = "H")]
    H,
}

impl QrEcc {
    /// Single-character wire representation (`"L"`, `"M"`, `"Q"`, `"H"`).
    pub fn as_str(self) -> &'static str {
        match self {
            QrEcc::L => "L",
            QrEcc::M => "M",
            QrEcc::Q => "Q",
            QrEcc::H => "H",
        }
    }
}

/// QR refresh / cache-bust token.
///
/// `On` sends `refresh=1`; `Value("v2")` sends `refresh=v2` verbatim. Use
/// [`QrRefresh::from_bool`] or [`QrRefresh::from_value`] for the common cases.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum QrRefresh {
    /// Plain boolean — serializes as `refresh=1`.
    On,
    /// Explicit value forwarded verbatim.
    Value(String),
}

impl QrRefresh {
    /// Build from a boolean — `true` becomes `On`, `false` is filtered out
    /// by callers (use `None` instead).
    pub fn from_bool(value: bool) -> Option<Self> {
        if value { Some(QrRefresh::On) } else { None }
    }

    /// Build from a non-empty string. Returns `None` for the empty string.
    pub fn from_value(value: impl Into<String>) -> Option<Self> {
        let s = value.into();
        if s.is_empty() {
            None
        } else {
            Some(QrRefresh::Value(s))
        }
    }

    /// Render to its wire representation.
    pub fn as_str(&self) -> &str {
        match self {
            QrRefresh::On => "1",
            QrRefresh::Value(s) => s.as_str(),
        }
    }
}

/// QR rendering options. All fields are optional — omit to use server defaults.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct QrOptions {
    /// Module size in pixels. Server clamps to 1–32. Default: 8.
    pub size: Option<u32>,
    /// Quiet-zone modules. Server clamps to 0–16. Default: 4.
    pub margin: Option<u32>,
    /// Error-correction level.
    pub ecc: Option<QrEcc>,
    /// Force the QR to encode a specific verified custom domain.
    pub domain: Option<String>,
    /// Cache-bust token.
    pub refresh: Option<QrRefresh>,
}

impl QrOptions {
    /// Build the empty options bag.
    pub fn new() -> Self {
        Self::default()
    }

    /// Builder — set `size`.
    #[must_use]
    pub fn with_size(mut self, size: u32) -> Self {
        self.size = Some(size);
        self
    }

    /// Builder — set `margin`.
    #[must_use]
    pub fn with_margin(mut self, margin: u32) -> Self {
        self.margin = Some(margin);
        self
    }

    /// Builder — set `ecc`.
    #[must_use]
    pub fn with_ecc(mut self, ecc: QrEcc) -> Self {
        self.ecc = Some(ecc);
        self
    }

    /// Builder — set `domain`.
    #[must_use]
    pub fn with_domain(mut self, domain: impl Into<String>) -> Self {
        self.domain = Some(domain.into());
        self
    }

    /// Builder — set `refresh`.
    #[must_use]
    pub fn with_refresh(mut self, refresh: QrRefresh) -> Self {
        self.refresh = Some(refresh);
        self
    }

    /// `true` when no options are set.
    pub fn is_empty(&self) -> bool {
        self.size.is_none()
            && self.margin.is_none()
            && self.ecc.is_none()
            && self.domain.is_none()
            && self.refresh.is_none()
    }

    /// Render this options bag into `(name, value)` pairs for a query string.
    pub fn to_query_pairs(&self) -> Vec<(&'static str, String)> {
        let mut out: Vec<(&'static str, String)> = Vec::new();
        if let Some(size) = self.size {
            out.push(("size", size.to_string()));
        }
        if let Some(margin) = self.margin {
            out.push(("margin", margin.to_string()));
        }
        if let Some(ecc) = self.ecc {
            out.push(("ecc", ecc.as_str().to_string()));
        }
        if let Some(ref domain) = self.domain {
            out.push(("domain", domain.clone()));
        }
        if let Some(ref refresh) = self.refresh {
            out.push(("refresh", refresh.as_str().to_string()));
        }
        out
    }
}

/// Query string for `GET /v1/links` — small helper used by [`crate::Links::list`].
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct ListLinksParams {
    /// Pagination cursor returned by a previous call.
    pub cursor: Option<i64>,
    /// Page size — server default applies when unset.
    pub limit: Option<u32>,
}
