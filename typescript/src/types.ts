/**
 * Public types returned by the Rerout API. Fields mirror the server-side
 * `LinkResponse` / `ProjectStatsResponse` etc. shapes so JSON is parsed
 * without transformation.
 */

export interface Tag {
  id: string
  name: string
  color: string
}

/** What a routing rule matches against. */
export type RoutingConditionType = 'country' | 'device'

/** How a routing rule compares the condition value. */
export type RoutingConditionOp = 'is' | 'is_not' | 'in'

/**
 * A geo/device routing rule (Smart Links). When the condition matches, the
 * redirect resolves to `target_url` instead of the link's default destination.
 */
export interface RoutingRule {
  /** What to match against: `country` or `device`. */
  condition_type: RoutingConditionType
  /** Comparison operator: `is`, `is_not`, or `in`. */
  condition_op: RoutingConditionOp
  /** Value(s) to compare against (e.g. `ZA`, `US,GB`, `mobile`). */
  condition_value: string
  /** Destination when the rule matches. */
  target_url: string
}

/** A weighted A/B destination (Smart Links), as returned by the API. */
export interface AbVariant {
  /** Stable variant id assigned by the server. */
  id: number
  /** Destination for this variant. */
  target_url: string
  /** Relative weight in the split. */
  weight: number
}

export interface Link {
  code: string
  short_url: string
  domain_hostname: string | null
  target_url: string
  project_id: string
  expires_at: number | null
  is_active: boolean
  seo_title: string | null
  seo_description: string | null
  seo_image_url: string | null
  seo_canonical_url: string | null
  seo_noindex: boolean
  seo_updated_at: number | null
  /** Read-only. Tags attached to this link. Empty on create; populated on get/list/update. */
  tags: Tag[]
  /** Smart Links: whether a password is required to follow this link. */
  password_protected: boolean
  /** Smart Links: click cap, or null when uncapped. */
  max_clicks: number | null
  /** Smart Links: total clicks recorded against this link. */
  click_count: number
  /** Smart Links: whether conversion tracking is enabled. */
  track_conversions: boolean
  /** Smart Links: ordered geo/device routing rules. */
  routing_rules: RoutingRule[]
  /** Smart Links: weighted A/B destinations. */
  ab_variants: AbVariant[]
  created_at: number
  updated_at: number
}

/** A weighted A/B destination as supplied on create/update. `weight` defaults server-side. */
export interface AbVariantInput {
  /** Destination for this variant. */
  target_url: string
  /** Relative weight in the split. Defaults server-side when omitted. */
  weight?: number
}

export interface CreateLinkInput {
  target_url: string
  /** Verified custom domain to host this link on. Omit for `rerout.co/:code`. */
  domain_hostname?: string
  /** Custom path. Only allowed with verified domain_hostname. */
  code?: string
  /** Unix seconds. Omit for a permanent link. */
  expires_at?: number
  /** Override social preview title. Max 90 chars. */
  seo_title?: string | null
  /** Override social preview description. Max 220 chars. */
  seo_description?: string | null
  /** Absolute https:// social preview image. */
  seo_image_url?: string | null
  /** Canonical URL for the preview HTML. */
  seo_canonical_url?: string | null
  /** Whether the preview page should be indexed. Default: false (noindex). */
  seo_noindex?: boolean
  /** Smart Links: plaintext password to gate the link. Hashed server-side. */
  password?: string
  /** Smart Links: cap the link to this many clicks. */
  max_clicks?: number
  /** Smart Links: mint a conversion click id on redirect. */
  track_conversions?: boolean
  /** Smart Links: ordered geo/device routing rules (full set). */
  routing_rules?: RoutingRule[]
  /** Smart Links: weighted A/B destinations (full set). */
  ab_variants?: AbVariantInput[]
}

export interface UpdateLinkInput {
  target_url?: string
  expires_at?: number | null
  is_active?: boolean
  seo_title?: string | null
  seo_description?: string | null
  seo_image_url?: string | null
  seo_canonical_url?: string | null
  seo_noindex?: boolean
  /** Smart Links: set a password, or `null` to clear it. */
  password?: string | null
  /** Smart Links: set a click cap, or `null` to clear it. */
  max_clicks?: number | null
  /** Smart Links: toggle conversion tracking. */
  track_conversions?: boolean
  /** Smart Links: full-replace the routing rules. */
  routing_rules?: RoutingRule[]
  /** Smart Links: full-replace the A/B variants. */
  ab_variants?: AbVariantInput[]
}

export interface ListLinksParams {
  /** Pagination cursor returned by a previous call. */
  cursor?: number
  /** Page size. Server-side default + max apply. */
  limit?: number
}

export interface ListLinksResult {
  links: Link[]
  next_cursor: number | null
}

export interface StatsBreakdown {
  value: string
  clicks: number
}

export interface DailyClicksPoint {
  day: number
  clicks: number
  qr_scans: number
}

export interface ProjectStats {
  days: number
  total_clicks: number
  qr_scans: number
  daily: DailyClicksPoint[]
  countries: StatsBreakdown[]
  referrers: StatsBreakdown[]
  devices: StatsBreakdown[]
  browsers: StatsBreakdown[]
  top_codes: StatsBreakdown[]
}

export interface LinkStats {
  code: string
  days: number
  total_clicks: number
  qr_scans: number
  countries: StatsBreakdown[]
  referrers: StatsBreakdown[]
}

export interface QrUrlOptions {
  /** Module size in px. 1–32. Server default: 8. */
  size?: number
  /** Quiet zone in modules. 0–16. Server default: 4. */
  margin?: number
  /** Error correction level. */
  ecc?: 'L' | 'M' | 'Q' | 'H'
  /** Force the QR to encode a specific verified custom domain. */
  domain?: string
  /** Cache-bust on regenerate. */
  refresh?: string | true
}

/** Delivery payload encoding for a webhook endpoint. */
export type WebhookPayloadFormat = 'json' | 'slack'

/** A webhook endpoint registered to the project. Mirrors `WebhookEndpointResponse`. */
export interface Webhook {
  id: string
  project_id: string
  name: string
  url: string
  events: string[]
  is_active: boolean
  payload_format: string
  created_at: number
  updated_at: number
  last_delivery_at: number | null
  last_success_at: number | null
  last_failure_at: number | null
}

export interface CreateWebhookInput {
  /** Human-readable label for the endpoint. */
  name: string
  /** Public https:// URL that receives signed POST deliveries. */
  url: string
  /** Event types to subscribe to (e.g. `link.created`). At least one. */
  events: string[]
  /** Whether the endpoint starts active. Default: true. */
  is_active?: boolean
  /** Payload encoding. Defaults to `json` (or `slack` for Slack URLs). */
  payload_format?: WebhookPayloadFormat
}

/**
 * Result of creating a webhook. The `signing_secret` (`whsec_…`) is returned
 * **once** — store it now; it is never shown again.
 */
export interface CreatedWebhook {
  endpoint: Webhook
  signing_secret: string
}

export interface ListWebhooksResult {
  endpoints: Webhook[]
  /** Every event type the server can deliver. */
  event_types: string[]
}

/** Body for recording a conversion against a prior click. */
export interface RecordConversionInput {
  /** The click id (`rrid`) minted on the tracked redirect. */
  click_id: string
  /** Conversion event label (e.g. `purchase`, `signup`). */
  event_name: string
  /** Optional monetary value in minor units (cents). */
  value_cents?: number
  /** Optional ISO 4217 currency code (e.g. `USD`). */
  currency?: string
}

/** Result of recording a conversion. */
export interface RecordedConversion {
  /** Whether the conversion is now recorded. */
  recorded: boolean
  /** Whether this `(click_id, event_name)` was already recorded (idempotent). */
  duplicate: boolean
}

/** A single link to create in a batch. */
export interface BatchLinkInput {
  target_url: string
  /** Custom path. Only allowed with a verified domain_hostname. */
  code?: string
  /** Unix seconds. Omit for a permanent link. */
  expires_at?: number
  /** Verified custom domain to host this link on. */
  domain_hostname?: string
}

/** Per-item outcome of a batch create. */
export interface BatchLinkResult {
  /** Index of the input item this result corresponds to. */
  index: number
  /** Whether the item was created. */
  ok: boolean
  /** Allocated code, when `ok`. */
  code?: string
  /** Failure reason, when not `ok`. */
  error?: string
}

/** Result of a batch link create (partial-success). */
export interface BatchCreateLinksResult {
  /** Number of links successfully created. */
  created: number
  /** Total number of items in the batch. */
  total: number
  /** Per-item outcomes, in input order. */
  results: BatchLinkResult[]
}
