/**
 * Error thrown for any Rerout API failure — network, HTTP non-2xx, or invalid
 * response shape. The `code` field matches the stable string identifier
 * returned by the API (e.g. `bad_target_url`, `rate_limited`, `not_found`) so
 * callers can branch on it without parsing the human-readable message.
 *
 * For network or non-JSON failures the `code` is set to one of the synthetic
 * values: `network_error`, `unexpected_response`, `client_error`.
 */
export class ReroutError extends Error {
  /** Stable error code, either from the API or a synthetic client-side one. */
  readonly code: string
  /** HTTP status code, or 0 when the request never reached the server. */
  readonly status: number
  /** The raw response body (parsed JSON or string), useful for debugging. */
  readonly details: unknown

  constructor(opts: {
    code: string
    message: string
    status: number
    details?: unknown
  }) {
    super(opts.message)
    this.name = 'ReroutError'
    this.code = opts.code
    this.status = opts.status
    this.details = opts.details
  }

  /** True for HTTP 5xx responses (server-side issues). */
  get isServerError(): boolean {
    return this.status >= 500 && this.status < 600
  }

  /** True for HTTP 429 — caller should back off and retry. */
  get isRateLimited(): boolean {
    return this.status === 429
  }
}
