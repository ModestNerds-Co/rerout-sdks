import type { QrUrlOptions } from './types.js'

/**
 * Build the URL the Rerout API serves the QR SVG from. This is a pure helper
 * — it does not call the API. Pass the resulting URL to an `<img>` tag,
 * download it server-side, or send it to a render pipeline.
 *
 * Authentication is the caller's responsibility — the endpoint is API-key
 * authenticated, so any `<img>` tag will need a way to send the bearer token
 * (typically via a server-side proxy).
 *
 * @example
 * ```ts
 * const url = buildQrUrl({ baseUrl: 'https://api.rerout.co', code: 'q4' })
 * // → https://api.rerout.co/v1/links/q4/qr
 *
 * const branded = buildQrUrl({
 *   baseUrl: 'https://api.rerout.co',
 *   code: 'q4',
 *   options: { size: 12, ecc: 'H', domain: 'go.brand.com' },
 * })
 * ```
 */
export function buildQrUrl(args: {
  baseUrl: string
  code: string
  options?: QrUrlOptions
}): string {
  const base = args.baseUrl.replace(/\/+$/, '')
  const url = new URL(`${base}/v1/links/${encodeURIComponent(args.code)}/qr`)
  const opts = args.options ?? {}
  if (opts.size !== undefined) url.searchParams.set('size', String(opts.size))
  if (opts.margin !== undefined) url.searchParams.set('margin', String(opts.margin))
  if (opts.ecc !== undefined) url.searchParams.set('ecc', opts.ecc)
  if (opts.domain !== undefined) url.searchParams.set('domain', opts.domain)
  if (opts.refresh !== undefined) {
    url.searchParams.set('refresh', opts.refresh === true ? '1' : opts.refresh)
  }
  return url.toString()
}
