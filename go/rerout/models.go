// Package rerout — request and response models.
//
// Field names follow the wire format (snake_case JSON tags). Optional fields
// use pointer types so the JSON encoder can omit them via `omitempty` —
// without pointers we couldn't tell "unset" from "zero value", which matters
// for booleans and ints in particular.

package rerout

import (
	"encoding/json"
	"strconv"
)

// Tag is a label attached to a link. Tags are read-only via the API-key
// client — they are returned in link responses but cannot be written.
type Tag struct {
	ID    string `json:"id"`
	Name  string `json:"name"`
	Color string `json:"color"`
}

// RoutingRule is one Smart-Links conditional redirect. When the inbound
// request matches the condition, the link routes to TargetURL instead of the
// link's default destination.
//
// ConditionType is "country" or "device"; ConditionOp is "is", "is_not", or
// "in". For the "in" operator, ConditionValue is a comma-separated list.
type RoutingRule struct {
	ConditionType  string `json:"condition_type"`
	ConditionOp    string `json:"condition_op"`
	ConditionValue string `json:"condition_value"`
	TargetURL      string `json:"target_url"`
}

// ABVariant is one weighted Smart-Links A/B testing destination. Inbound
// traffic is split across variants in proportion to their Weight.
//
// ID is server-assigned and read-only on responses. On CreateABVariantInput it
// is absent.
type ABVariant struct {
	ID        int64  `json:"id"`
	TargetURL string `json:"target_url"`
	Weight    int    `json:"weight"`
}

// Link is the canonical short-link representation returned by the API.
type Link struct {
	Code            string  `json:"code"`
	ShortURL        string  `json:"short_url"`
	DomainHostname  *string `json:"domain_hostname,omitempty"`
	TargetURL       string  `json:"target_url"`
	ProjectID       string  `json:"project_id"`
	ExpiresAt       *int64  `json:"expires_at,omitempty"`
	IsActive        bool    `json:"is_active"`
	SEOTitle        *string `json:"seo_title,omitempty"`
	SEODescription  *string `json:"seo_description,omitempty"`
	SEOImageURL     *string `json:"seo_image_url,omitempty"`
	SEOCanonicalURL *string `json:"seo_canonical_url,omitempty"`
	SEONoindex      bool    `json:"seo_noindex"`
	SEOUpdatedAt    *int64  `json:"seo_updated_at,omitempty"`
	Tags            []Tag   `json:"tags"`

	// Smart Links fields.

	// PasswordProtected reports whether the link requires a password before it
	// redirects. The password itself is never returned.
	PasswordProtected bool `json:"password_protected"`
	// MaxClicks is the click cap after which the link stops redirecting. Nil
	// for an uncapped link.
	MaxClicks *int64 `json:"max_clicks,omitempty"`
	// ClickCount is the number of clicks recorded against the link so far.
	ClickCount int64 `json:"click_count"`
	// TrackConversions reports whether conversion tracking is enabled.
	TrackConversions bool `json:"track_conversions"`
	// RoutingRules are the conditional redirect rules, evaluated in order.
	RoutingRules []RoutingRule `json:"routing_rules"`
	// ABVariants are the weighted A/B testing destinations.
	ABVariants []ABVariant `json:"ab_variants"`

	CreatedAt int64 `json:"created_at"`
	UpdatedAt int64 `json:"updated_at"`
}

// CreateABVariantInput is one weighted A/B variant on CreateLinkInput /
// UpdateLinkInput. Weight is optional — the server applies an even split when
// it is unset.
type CreateABVariantInput struct {
	TargetURL string `json:"target_url"`
	Weight    *int   `json:"weight,omitempty"`
}

// CreateLinkInput is the body for POST /v1/links.
//
// Optional fields are pointers so the JSON encoder omits unset ones. Use the
// String / Int64 / Bool helpers for clear call sites:
//
//	in := rerout.CreateLinkInput{
//	    TargetURL:      "https://example.com/q4",
//	    DomainHostname: rerout.String("go.brand.com"),
//	    Code:           rerout.String("q4"),
//	    SEONoindex:     rerout.Bool(false),
//	}
type CreateLinkInput struct {
	TargetURL       string  `json:"target_url"`
	DomainHostname  *string `json:"domain_hostname,omitempty"`
	Code            *string `json:"code,omitempty"`
	ExpiresAt       *int64  `json:"expires_at,omitempty"`
	SEOTitle        *string `json:"seo_title,omitempty"`
	SEODescription  *string `json:"seo_description,omitempty"`
	SEOImageURL     *string `json:"seo_image_url,omitempty"`
	SEOCanonicalURL *string `json:"seo_canonical_url,omitempty"`
	SEONoindex      *bool   `json:"seo_noindex,omitempty"`

	// Smart Links fields. All optional.

	// Password, when set, requires visitors to enter it before redirecting.
	Password *string `json:"password,omitempty"`
	// MaxClicks caps the number of redirects. Omit for an uncapped link.
	MaxClicks *int64 `json:"max_clicks,omitempty"`
	// TrackConversions enables conversion tracking for the link.
	TrackConversions *bool `json:"track_conversions,omitempty"`
	// RoutingRules are conditional redirect rules, evaluated in order.
	RoutingRules []RoutingRule `json:"routing_rules,omitempty"`
	// ABVariants are weighted A/B testing destinations.
	ABVariants []CreateABVariantInput `json:"ab_variants,omitempty"`
}

// UpdateLinkInput is the body for PATCH /v1/links/:code.
//
// Every field is optional. The struct distinguishes "leave the field alone"
// from "set the field to null on the server" via the ClearXxx flag pattern:
//
//	rerout.UpdateLinkInput{
//	    TargetURL:      rerout.String("https://example.com/new"),
//	    ClearExpiresAt: true,   // sends "expires_at": null
//	    SEOTitle:       rerout.String("Black Friday"), // sets the title
//	    ClearSEOImageURL: true, // wipes the image
//	}
//
// MarshalJSON emits explicit nulls for cleared fields and omits unset ones.
// An empty payload (no field set, no clear flag) is a client-side error —
// Client.Links.Update rejects it without hitting the API.
type UpdateLinkInput struct {
	TargetURL *string `json:"-"`

	ExpiresAt      *int64 `json:"-"`
	ClearExpiresAt bool   `json:"-"`

	IsActive *bool `json:"-"`

	SEOTitle      *string `json:"-"`
	ClearSEOTitle bool    `json:"-"`

	SEODescription      *string `json:"-"`
	ClearSEODescription bool    `json:"-"`

	SEOImageURL      *string `json:"-"`
	ClearSEOImageURL bool    `json:"-"`

	SEOCanonicalURL      *string `json:"-"`
	ClearSEOCanonicalURL bool    `json:"-"`

	SEONoindex *bool `json:"-"`

	// Smart Links fields.

	// Password sets a new password; ClearPassword removes password protection
	// (sends "password": null). ClearPassword wins over Password.
	Password      *string `json:"-"`
	ClearPassword bool    `json:"-"`

	// MaxClicks sets a new click cap; ClearMaxClicks removes the cap (sends
	// "max_clicks": null). ClearMaxClicks wins over MaxClicks.
	MaxClicks      *int64 `json:"-"`
	ClearMaxClicks bool   `json:"-"`

	// TrackConversions toggles conversion tracking.
	TrackConversions *bool `json:"-"`

	// RoutingRules, when non-nil, fully replaces the link's routing rules. A
	// non-nil empty slice clears all rules. Nil leaves them untouched.
	RoutingRules *[]RoutingRule `json:"-"`

	// ABVariants, when non-nil, fully replaces the link's A/B variants. A
	// non-nil empty slice clears all variants. Nil leaves them untouched.
	ABVariants *[]CreateABVariantInput `json:"-"`
}

// MarshalJSON implements json.Marshaler for UpdateLinkInput.
//
// It walks each field, emitting:
//   - the value when the field is set,
//   - an explicit `null` when the matching ClearXxx flag is true,
//   - nothing when neither — so server-side merge semantics leave the field
//     untouched.
func (u UpdateLinkInput) MarshalJSON() ([]byte, error) {
	out := make(map[string]any, 8)
	if u.TargetURL != nil {
		out["target_url"] = *u.TargetURL
	}
	if u.ClearExpiresAt {
		out["expires_at"] = nil
	} else if u.ExpiresAt != nil {
		out["expires_at"] = *u.ExpiresAt
	}
	if u.IsActive != nil {
		out["is_active"] = *u.IsActive
	}
	if u.ClearSEOTitle {
		out["seo_title"] = nil
	} else if u.SEOTitle != nil {
		out["seo_title"] = *u.SEOTitle
	}
	if u.ClearSEODescription {
		out["seo_description"] = nil
	} else if u.SEODescription != nil {
		out["seo_description"] = *u.SEODescription
	}
	if u.ClearSEOImageURL {
		out["seo_image_url"] = nil
	} else if u.SEOImageURL != nil {
		out["seo_image_url"] = *u.SEOImageURL
	}
	if u.ClearSEOCanonicalURL {
		out["seo_canonical_url"] = nil
	} else if u.SEOCanonicalURL != nil {
		out["seo_canonical_url"] = *u.SEOCanonicalURL
	}
	if u.SEONoindex != nil {
		out["seo_noindex"] = *u.SEONoindex
	}
	if u.ClearPassword {
		out["password"] = nil
	} else if u.Password != nil {
		out["password"] = *u.Password
	}
	if u.ClearMaxClicks {
		out["max_clicks"] = nil
	} else if u.MaxClicks != nil {
		out["max_clicks"] = *u.MaxClicks
	}
	if u.TrackConversions != nil {
		out["track_conversions"] = *u.TrackConversions
	}
	if u.RoutingRules != nil {
		out["routing_rules"] = *u.RoutingRules
	}
	if u.ABVariants != nil {
		out["ab_variants"] = *u.ABVariants
	}
	return json.Marshal(out)
}

// IsEmpty reports whether no field is set and no clear flag is true.
// Sending an empty PATCH is a client-side error.
func (u UpdateLinkInput) IsEmpty() bool {
	if u.TargetURL != nil ||
		u.ExpiresAt != nil || u.ClearExpiresAt ||
		u.IsActive != nil ||
		u.SEOTitle != nil || u.ClearSEOTitle ||
		u.SEODescription != nil || u.ClearSEODescription ||
		u.SEOImageURL != nil || u.ClearSEOImageURL ||
		u.SEOCanonicalURL != nil || u.ClearSEOCanonicalURL ||
		u.SEONoindex != nil ||
		u.Password != nil || u.ClearPassword ||
		u.MaxClicks != nil || u.ClearMaxClicks ||
		u.TrackConversions != nil ||
		u.RoutingRules != nil ||
		u.ABVariants != nil {
		return false
	}
	return true
}

// ListLinksResult is one page of links returned by GET /v1/links.
type ListLinksResult struct {
	Links      []Link `json:"links"`
	NextCursor *int64 `json:"next_cursor,omitempty"`
}

// HasMore reports whether NextCursor is set — i.e. another page is available.
func (r ListLinksResult) HasMore() bool { return r.NextCursor != nil }

// StatsBreakdown is one bucket in an analytics breakdown — one country, one
// device class, one referrer, etc.
type StatsBreakdown struct {
	Value  string `json:"value"`
	Clicks int64  `json:"clicks"`
}

// DailyClicksPoint is one point in a daily-clicks time series.
type DailyClicksPoint struct {
	Day     int64 `json:"day"`
	Clicks  int64 `json:"clicks"`
	QRScans int64 `json:"qr_scans"`
}

// ProjectStats is the response from GET /v1/projects/me/stats.
type ProjectStats struct {
	Days        int                `json:"days"`
	TotalClicks int64              `json:"total_clicks"`
	QRScans     int64              `json:"qr_scans"`
	Daily       []DailyClicksPoint `json:"daily"`
	Countries   []StatsBreakdown   `json:"countries"`
	Referrers   []StatsBreakdown   `json:"referrers"`
	Devices     []StatsBreakdown   `json:"devices"`
	Browsers    []StatsBreakdown   `json:"browsers"`
	TopCodes    []StatsBreakdown   `json:"top_codes"`
}

// LinkStats is the response from GET /v1/links/:code/stats.
type LinkStats struct {
	Code        string           `json:"code"`
	Days        int              `json:"days"`
	TotalClicks int64            `json:"total_clicks"`
	QRScans     int64            `json:"qr_scans"`
	Countries   []StatsBreakdown `json:"countries"`
	Referrers   []StatsBreakdown `json:"referrers"`
}

// Project is the response from GET /v1/projects/me.
type Project struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	Slug string `json:"slug"`
}

// DeleteResult is the response body from DELETE /v1/links/:code and
// DELETE /v1/projects/me/webhooks/:id.
type DeleteResult struct {
	Deleted bool `json:"deleted"`
}

// ─── Conversions ─────────────────────────────────────────────────────────

// RecordConversionInput is the body for POST /v1/conversions.
//
// ClickID and EventName are required. ValueCents and Currency are optional —
// pointers so they are omitted when unset.
//
//	in := rerout.RecordConversionInput{
//	    ClickID:    "clk_123",
//	    EventName:  "purchase",
//	    ValueCents: rerout.Int64(4999),
//	    Currency:   rerout.String("USD"),
//	}
type RecordConversionInput struct {
	ClickID    string  `json:"click_id"`
	EventName  string  `json:"event_name"`
	ValueCents *int64  `json:"value_cents,omitempty"`
	Currency   *string `json:"currency,omitempty"`
}

// ConversionResult is the response from POST /v1/conversions.
//
// Duplicate is true when the (click_id, event_name) pair was already recorded —
// the call is idempotent, so Recorded reflects whether a new row was written.
type ConversionResult struct {
	Recorded  bool `json:"recorded"`
	Duplicate bool `json:"duplicate"`
}

// ─── Batch ───────────────────────────────────────────────────────────────

// BatchLinkInput is one entry in a CreateLinks batch. TargetURL is required;
// the rest are optional pointers omitted when unset.
type BatchLinkInput struct {
	TargetURL      string  `json:"target_url"`
	Code           *string `json:"code,omitempty"`
	ExpiresAt      *int64  `json:"expires_at,omitempty"`
	DomainHostname *string `json:"domain_hostname,omitempty"`
}

// BatchLinkResult is the per-item outcome of a CreateLinks batch. Index is the
// zero-based position of the input link. OK reports success; on success Code
// is populated, otherwise Error carries the failure reason.
type BatchLinkResult struct {
	Index int     `json:"index"`
	OK    bool    `json:"ok"`
	Code  *string `json:"code,omitempty"`
	Error *string `json:"error,omitempty"`
}

// BatchCreateLinksResult is the response from POST /v1/links/batch.
type BatchCreateLinksResult struct {
	Created int               `json:"created"`
	Total   int               `json:"total"`
	Results []BatchLinkResult `json:"results"`
}

// Webhook is a webhook endpoint registered to the project. Mirrors the
// server-side WebhookEndpointResponse shape.
type Webhook struct {
	ID             string   `json:"id"`
	ProjectID      string   `json:"project_id"`
	Name           string   `json:"name"`
	URL            string   `json:"url"`
	Events         []string `json:"events"`
	IsActive       bool     `json:"is_active"`
	PayloadFormat  string   `json:"payload_format"`
	CreatedAt      int64    `json:"created_at"`
	UpdatedAt      int64    `json:"updated_at"`
	LastDeliveryAt *int64   `json:"last_delivery_at,omitempty"`
	LastSuccessAt  *int64   `json:"last_success_at,omitempty"`
	LastFailureAt  *int64   `json:"last_failure_at,omitempty"`
}

// CreateWebhookInput is the body for POST /v1/projects/me/webhooks.
//
// Name, URL, and Events are required. Optional fields are pointers so the
// JSON encoder omits unset ones — letting the server apply its defaults
// (IsActive defaults to true; PayloadFormat defaults to "json").
//
//	in := rerout.CreateWebhookInput{
//	    Name:          "Order events",
//	    URL:           "https://example.com/hooks/rerout",
//	    Events:        []string{"link.created", "link.clicked"},
//	    IsActive:      rerout.Bool(true),
//	    PayloadFormat: rerout.String("json"),
//	}
type CreateWebhookInput struct {
	Name          string   `json:"name"`
	URL           string   `json:"url"`
	Events        []string `json:"events"`
	IsActive      *bool    `json:"is_active,omitempty"`
	PayloadFormat *string  `json:"payload_format,omitempty"`
}

// CreatedWebhook is the response from POST /v1/projects/me/webhooks.
//
// SigningSecret (a "whsec_…" value) is returned ONCE — persist it now so you
// can verify deliveries with VerifySignature; it is never shown again.
type CreatedWebhook struct {
	Endpoint      Webhook `json:"endpoint"`
	SigningSecret string  `json:"signing_secret"`
}

// ListWebhooksResult is the response from GET /v1/projects/me/webhooks.
type ListWebhooksResult struct {
	Endpoints []Webhook `json:"endpoints"`
	// EventTypes lists every event type the server can deliver.
	EventTypes []string `json:"event_types"`
}

// ListLinksParams are the optional cursor / limit query parameters for
// Links.List.
type ListLinksParams struct {
	// Cursor is the pagination cursor returned by a previous List call.
	Cursor *int64
	// Limit is the page size. Server-side default and max apply.
	Limit *int
}

// QROptions controls the QR rendering parameters for Client.QR.URL /
// Client.QR.SVG.
//
// All fields are optional — leave them unset to use the server defaults.
type QROptions struct {
	// Size is the module size in pixels. 1-32. Server default: 8.
	Size *int
	// Margin is the quiet zone in modules. 0-16. Server default: 4.
	Margin *int
	// ECC is the error-correction level: "L", "M", "Q", or "H".
	ECC string
	// Domain forces the QR to encode a specific verified custom domain.
	Domain string
	// Refresh is a cache-bust token. RefreshTrue == true sends "refresh=1";
	// otherwise Refresh is forwarded verbatim and triggers a fresh render.
	Refresh     string
	RefreshTrue bool
}

// toQueryParameters renders the options bag into URL query pairs in a
// stable order. Empty result when nothing is set.
func (o *QROptions) toQueryParameters() map[string]string {
	if o == nil {
		return nil
	}
	out := make(map[string]string, 5)
	if o.Size != nil {
		out["size"] = strconv.Itoa(*o.Size)
	}
	if o.Margin != nil {
		out["margin"] = strconv.Itoa(*o.Margin)
	}
	if o.ECC != "" {
		out["ecc"] = o.ECC
	}
	if o.Domain != "" {
		out["domain"] = o.Domain
	}
	switch {
	case o.RefreshTrue:
		out["refresh"] = "1"
	case o.Refresh != "":
		out["refresh"] = o.Refresh
	}
	return out
}

// ─── pointer helpers ─────────────────────────────────────────────────────

// String returns a pointer to v. Use in struct literals where an optional
// *string is required.
func String(v string) *string { return &v }

// Int returns a pointer to v.
func Int(v int) *int { return &v }

// Int64 returns a pointer to v.
func Int64(v int64) *int64 { return &v }

// Bool returns a pointer to v.
func Bool(v bool) *bool { return &v }
