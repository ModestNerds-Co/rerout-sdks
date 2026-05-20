import { createHmac, timingSafeEqual } from 'node:crypto'

/**
 * Default tolerance window (in seconds) between the `t=` timestamp in the
 * signature header and the current time. Any signed payload older than this
 * is rejected — protects against captured-replay attacks. Five minutes.
 */
export const DEFAULT_SIGNATURE_TOLERANCE_SECONDS = 300

export interface VerifyOptions {
  /** Raw, unmodified request body. Reading as `await req.text()` or equivalent. */
  rawBody: string
  /** Value of the `X-Rerout-Signature` header. */
  signatureHeader: string
  /** Endpoint signing secret (`whsec_…`) from the dashboard. */
  secret: string
  /** Window in seconds; defaults to 300. Set 0 to disable timestamp check. */
  toleranceSeconds?: number
  /**
   * Injectable clock for testing. Returns the current time in unix seconds.
   * Defaults to `Math.floor(Date.now() / 1000)`.
   */
  now?: () => number
}

/**
 * Verify a Rerout webhook signature.
 *
 * The header format is `t={unix},v1={hex_hmac}`. The HMAC is computed over
 * `${timestamp}.${rawBody}` with the endpoint signing secret as the key.
 *
 * Returns `true` only when:
 * - the header parses cleanly,
 * - the timestamp is within `toleranceSeconds` of `now()`,
 * - and the computed HMAC matches the supplied `v1` in constant time.
 *
 * @example
 * ```ts
 * import { verifyReroutSignature } from '@rerout/sdk'
 *
 * app.post('/webhooks/rerout', async (req, res) => {
 *   const raw = await readRawBody(req)
 *   const ok = verifyReroutSignature({
 *     rawBody: raw,
 *     signatureHeader: req.headers['x-rerout-signature'] as string,
 *     secret: process.env.REROUT_WEBHOOK_SECRET!,
 *   })
 *   if (!ok) return res.status(400).end('bad signature')
 *   // handle event…
 *   res.status(200).end()
 * })
 * ```
 */
export function verifyReroutSignature(opts: VerifyOptions): boolean {
  if (!opts.signatureHeader || !opts.secret || opts.rawBody === undefined) {
    return false
  }
  const parts = parseSignatureHeader(opts.signatureHeader)
  if (!parts) return false

  const tolerance = opts.toleranceSeconds ?? DEFAULT_SIGNATURE_TOLERANCE_SECONDS
  if (tolerance > 0) {
    const now = opts.now ? opts.now() : Math.floor(Date.now() / 1000)
    if (Math.abs(now - parts.timestamp) > tolerance) return false
  }

  const expectedHex = createHmac('sha256', opts.secret)
    .update(`${parts.timestamp}.${opts.rawBody}`)
    .digest('hex')

  const expected = safeFromHex(expectedHex)
  const actual = safeFromHex(parts.v1)
  if (!expected || !actual || expected.length !== actual.length) return false
  return timingSafeEqual(expected, actual)
}

interface ParsedSignature {
  timestamp: number
  v1: string
}

function parseSignatureHeader(header: string): ParsedSignature | null {
  let timestamp: number | undefined
  let v1: string | undefined
  for (const segment of header.split(',')) {
    const eq = segment.indexOf('=')
    if (eq <= 0) continue
    const key = segment.slice(0, eq).trim().toLowerCase()
    const value = segment.slice(eq + 1).trim()
    if (key === 't') {
      const parsed = Number.parseInt(value, 10)
      if (Number.isFinite(parsed) && parsed > 0) timestamp = parsed
    } else if (key === 'v1') {
      v1 = value
    }
  }
  if (timestamp === undefined || !v1) return null
  return { timestamp, v1 }
}

function safeFromHex(hex: string): Buffer | null {
  if (hex.length === 0 || hex.length % 2 !== 0) return null
  if (!/^[0-9a-fA-F]+$/.test(hex)) return null
  return Buffer.from(hex, 'hex')
}
