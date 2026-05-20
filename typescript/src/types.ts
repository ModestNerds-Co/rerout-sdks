/**
 * Public types returned by the Rerout API. Fields mirror the server-side
 * `LinkResponse` / `ProjectStatsResponse` etc. shapes so JSON is parsed
 * without transformation.
 */

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
