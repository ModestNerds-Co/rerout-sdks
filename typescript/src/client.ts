import { ReroutError } from './errors.js'
import { buildQrUrl } from './qr.js'
import type {
  BatchCreateLinksResult,
  BatchLinkInput,
  CreateLinkInput,
  CreatedWebhook,
  CreateTagInput,
  CreateWebhookInput,
  Link,
  LinkStats,
  ListLinksParams,
  ListLinksResult,
  ListTagsResult,
  ListWebhooksResult,
  ProjectStats,
  QrUrlOptions,
  RecordConversionInput,
  RecordedConversion,
  Tag,
  UpdateLinkInput,
  UpdateTagInput,
} from './types.js'

/** Default production API URL. Override via `baseUrl` for staging / self-hosted. */
export const DEFAULT_BASE_URL = 'https://api.rerout.co'

export interface ReroutClientOptions {
  /** Project API key (`rrk_…`). Required. */
  apiKey: string
  /** Override the API base URL. Defaults to `https://api.rerout.co`. */
  baseUrl?: string
  /** Inject a custom `fetch` implementation. Useful in tests and edge runtimes. */
  fetch?: typeof fetch
  /** Request timeout in ms. Defaults to 30 000. */
  timeoutMs?: number
  /** Additional headers added to every request (e.g. `user-agent`). */
  defaultHeaders?: Record<string, string>
}

interface RequestInitLike {
  method: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  path: string
  query?: Record<string, string | number | undefined>
  body?: unknown
}

/**
 * Official client for the Rerout API.
 *
 * @example
 * ```ts
 * import { Rerout } from '@rerout/sdk'
 *
 * const rerout = new Rerout({ apiKey: process.env.REROUT_API_KEY! })
 *
 * const link = await rerout.links.create({
 *   target_url: 'https://example.com/sale',
 * })
 * console.log(link.short_url)
 * ```
 */
export class Rerout {
  /** Link operations: create, list, get, update, delete, stats. */
  readonly links: Links
  /** Project-level operations: aggregate stats. */
  readonly project: Project
  /** QR helpers (pure URL builders + signed fetch). */
  readonly qr: Qr
  /** Webhook endpoint management: create, list, delete. */
  readonly webhooks: Webhooks
  /** Conversion tracking: record a conversion against a prior click. */
  readonly conversions: Conversions
  /** Tag management: list, create, update, delete. */
  readonly tags: Tags

  private readonly apiKey: string
  private readonly baseUrl: string
  private readonly fetchImpl: typeof fetch
  private readonly timeoutMs: number
  private readonly defaultHeaders: Record<string, string>

  constructor(options: ReroutClientOptions) {
    if (!options.apiKey || typeof options.apiKey !== 'string') {
      throw new ReroutError({
        code: 'missing_api_key',
        message: 'A project API key is required to construct Rerout.',
        status: 0,
      })
    }
    this.apiKey = options.apiKey
    this.baseUrl = (options.baseUrl ?? DEFAULT_BASE_URL).replace(/\/+$/, '')
    this.fetchImpl =
      options.fetch ??
      (typeof fetch !== 'undefined'
        ? fetch
        : (() => {
            throw new ReroutError({
              code: 'missing_fetch',
              message:
                'No global fetch available. Pass `fetch` in ReroutClientOptions or run on Node 18+.',
              status: 0,
            })
          })())
    this.timeoutMs = options.timeoutMs ?? 30_000
    this.defaultHeaders = options.defaultHeaders ?? {}

    this.links = new Links(this)
    this.project = new Project(this)
    this.qr = new Qr(this)
    this.webhooks = new Webhooks(this)
    this.conversions = new Conversions(this)
    this.tags = new Tags(this)
  }

  /** @internal — invoked by Links / Project / Qr. */
  async request<T>(init: RequestInitLike): Promise<T> {
    const url = new URL(this.baseUrl + init.path)
    if (init.query) {
      for (const [k, v] of Object.entries(init.query)) {
        if (v !== undefined && v !== null) url.searchParams.set(k, String(v))
      }
    }
    const headers: Record<string, string> = {
      authorization: `Bearer ${this.apiKey}`,
      accept: 'application/json',
      ...this.defaultHeaders,
    }
    const body = init.body === undefined ? undefined : JSON.stringify(init.body)
    if (body !== undefined) headers['content-type'] = 'application/json'

    const abort = new AbortController()
    const timer = setTimeout(() => abort.abort(), this.timeoutMs)

    let response: Response
    try {
      response = await this.fetchImpl(url.toString(), {
        method: init.method,
        headers,
        body,
        signal: abort.signal,
      })
    } catch (error) {
      throw new ReroutError({
        code: abort.signal.aborted ? 'timeout' : 'network_error',
        message:
          error instanceof Error
            ? error.message
            : 'Request to Rerout failed before the server replied.',
        status: 0,
        details: error,
      })
    } finally {
      clearTimeout(timer)
    }

    const text = await response.text()
    if (!response.ok) throw parseError(response.status, text)

    if (text.length === 0) return undefined as T
    try {
      return JSON.parse(text) as T
    } catch (error) {
      throw new ReroutError({
        code: 'unexpected_response',
        message: 'Rerout returned a non-JSON success body.',
        status: response.status,
        details: { body: text, error },
      })
    }
  }

  /** Internal — used by Qr to expose the resolved base URL. */
  get resolvedBaseUrl(): string {
    return this.baseUrl
  }
}

function parseError(status: number, body: string): ReroutError {
  if (body.length === 0) {
    return new ReroutError({
      code: synthCodeForStatus(status),
      message: `Rerout returned HTTP ${status} with no body.`,
      status,
    })
  }
  try {
    const parsed = JSON.parse(body) as { code?: string; message?: string }
    return new ReroutError({
      code: parsed.code ?? synthCodeForStatus(status),
      message: parsed.message ?? `Rerout returned HTTP ${status}.`,
      status,
      details: parsed,
    })
  } catch {
    return new ReroutError({
      code: synthCodeForStatus(status),
      message: `Rerout returned HTTP ${status} (non-JSON body).`,
      status,
      details: { body },
    })
  }
}

function synthCodeForStatus(status: number): string {
  if (status === 401) return 'unauthorized'
  if (status === 403) return 'forbidden'
  if (status === 404) return 'not_found'
  if (status === 429) return 'rate_limited'
  if (status >= 500) return 'server_error'
  return 'client_error'
}

// ─── Namespaces ─────────────────────────────────────────────────────────────

export class Links {
  /** @internal */
  constructor(private readonly client: Rerout) {}

  /** Create a new short link. */
  create(input: CreateLinkInput): Promise<Link> {
    return this.client.request<Link>({
      method: 'POST',
      path: '/v1/links',
      body: input,
    })
  }

  /**
   * Bulk-create links in one request. Each item succeeds or fails
   * independently; inspect `results[i].ok` for per-item outcomes.
   */
  createBatch(links: BatchLinkInput[]): Promise<BatchCreateLinksResult> {
    return this.client.request<BatchCreateLinksResult>({
      method: 'POST',
      path: '/v1/links/batch',
      body: { links },
    })
  }

  /** Paginated list of links in the project. */
  list(params?: ListLinksParams): Promise<ListLinksResult> {
    return this.client.request<ListLinksResult>({
      method: 'GET',
      path: '/v1/links',
      query: params as Record<string, string | number | undefined> | undefined,
    })
  }

  /** Get a single link by code. */
  get(code: string): Promise<Link> {
    return this.client.request<Link>({
      method: 'GET',
      path: `/v1/links/${encodeURIComponent(code)}`,
    })
  }

  /** Patch a link. Only fields present in `input` are changed. */
  update(code: string, input: UpdateLinkInput): Promise<Link> {
    return this.client.request<Link>({
      method: 'PATCH',
      path: `/v1/links/${encodeURIComponent(code)}`,
      body: input,
    })
  }

  /** Soft-delete a link. The short URL stops redirecting and is gone from lists. */
  delete(code: string): Promise<{ deleted: boolean }> {
    return this.client.request<{ deleted: boolean }>({
      method: 'DELETE',
      path: `/v1/links/${encodeURIComponent(code)}`,
    })
  }

  /** Per-link click stats. Defaults to 30 days. */
  stats(code: string, days = 30): Promise<LinkStats> {
    return this.client.request<LinkStats>({
      method: 'GET',
      path: `/v1/links/${encodeURIComponent(code)}/stats`,
      query: { days },
    })
  }
}

export class Project {
  /** @internal */
  constructor(private readonly client: Rerout) {}

  /** Aggregate stats across every link in the project. */
  stats(days = 30): Promise<ProjectStats> {
    return this.client.request<ProjectStats>({
      method: 'GET',
      path: '/v1/projects/me/stats',
      query: { days },
    })
  }

  /** Info about the project that owns the current API key. */
  me(): Promise<{ id: string; name: string; slug: string }> {
    return this.client.request<{ id: string; name: string; slug: string }>({
      method: 'GET',
      path: '/v1/projects/me',
    })
  }
}

export class Webhooks {
  /** @internal */
  constructor(private readonly client: Rerout) {}

  /**
   * Create a webhook endpoint for the project that owns the API key. The
   * returned `signing_secret` is shown once — persist it to verify deliveries.
   */
  create(input: CreateWebhookInput): Promise<CreatedWebhook> {
    return this.client.request<CreatedWebhook>({
      method: 'POST',
      path: '/v1/projects/me/webhooks',
      body: input,
    })
  }

  /** List webhook endpoints and the event types the server can deliver. */
  list(): Promise<ListWebhooksResult> {
    return this.client.request<ListWebhooksResult>({
      method: 'GET',
      path: '/v1/projects/me/webhooks',
    })
  }

  /** Soft-delete an endpoint and abandon its pending deliveries. */
  delete(endpointId: string): Promise<{ deleted: boolean }> {
    return this.client.request<{ deleted: boolean }>({
      method: 'DELETE',
      path: `/v1/projects/me/webhooks/${encodeURIComponent(endpointId)}`,
    })
  }
}

export class Conversions {
  /** @internal */
  constructor(private readonly client: Rerout) {}

  /**
   * Record a conversion attributed to a prior click via its `click_id`
   * (`rrid`). Idempotent per `(click_id, event_name)`.
   */
  record(input: RecordConversionInput): Promise<RecordedConversion> {
    return this.client.request<RecordedConversion>({
      method: 'POST',
      path: '/v1/conversions',
      body: input,
    })
  }
}

export class Tags {
  /** @internal */
  constructor(private readonly client: Rerout) {}

  /** List the project's tags with their live link counts. */
  list(): Promise<ListTagsResult> {
    return this.client.request<ListTagsResult>({
      method: 'GET',
      path: '/v1/projects/me/tags',
    })
  }

  /** Create a tag. `color` is optional; the server defaults it. */
  create(input: CreateTagInput): Promise<Tag> {
    return this.client.request<Tag>({
      method: 'POST',
      path: '/v1/projects/me/tags',
      body: input,
    })
  }

  /**
   * Update a tag's name and/or color. Mirrors `links.update`: omitted fields are
   * left unchanged; the server rejects a fully empty patch.
   */
  update(tagId: string, input: UpdateTagInput): Promise<Tag> {
    return this.client.request<Tag>({
      method: 'PATCH',
      path: `/v1/projects/me/tags/${encodeURIComponent(tagId)}`,
      body: input,
    })
  }

  /** Delete a tag and drop its assignments from all links. */
  delete(tagId: string): Promise<{ deleted: boolean }> {
    return this.client.request<{ deleted: boolean }>({
      method: 'DELETE',
      path: `/v1/projects/me/tags/${encodeURIComponent(tagId)}`,
    })
  }
}

export class Qr {
  /** @internal */
  constructor(private readonly client: Rerout) {}

  /**
   * Build the URL the API serves the QR SVG from. Pure — does not call the API.
   */
  url(code: string, options?: QrUrlOptions): string {
    return buildQrUrl({
      baseUrl: this.client.resolvedBaseUrl,
      code,
      options,
    })
  }

  /**
   * Fetch the QR as an SVG string. Hits the same endpoint as `url()` but
   * attaches the bearer token and returns the rendered body.
   */
  async svg(code: string, options?: QrUrlOptions): Promise<string> {
    return this.client.request<string>({
      method: 'GET',
      path:
        `/v1/links/${encodeURIComponent(code)}/qr` +
        qrQueryString(options),
    })
  }
}

function qrQueryString(options?: QrUrlOptions): string {
  if (!options) return ''
  const params = new URLSearchParams()
  if (options.size !== undefined) params.set('size', String(options.size))
  if (options.margin !== undefined) params.set('margin', String(options.margin))
  if (options.ecc !== undefined) params.set('ecc', options.ecc)
  if (options.domain !== undefined) params.set('domain', options.domain)
  if (options.refresh !== undefined) {
    params.set('refresh', options.refresh === true ? '1' : options.refresh)
  }
  const qs = params.toString()
  return qs.length === 0 ? '' : `?${qs}`
}
