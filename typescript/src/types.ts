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
  created_at: number
  updated_at: number
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
