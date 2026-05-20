import { describe, expect, it, vi } from 'vitest'
import { Rerout, buildQrUrl } from '../src/index.js'

describe('buildQrUrl', () => {
  it('returns the basic path with no options', () => {
    expect(buildQrUrl({ baseUrl: 'https://api.rerout.co', code: 'q4' })).toBe(
      'https://api.rerout.co/v1/links/q4/qr',
    )
  })

  it('appends every option that is set', () => {
    const url = buildQrUrl({
      baseUrl: 'https://api.rerout.co',
      code: 'q4',
      options: { size: 12, margin: 2, ecc: 'H', domain: 'go.brand.com', refresh: true },
    })
    const parsed = new URL(url)
    expect(parsed.pathname).toBe('/v1/links/q4/qr')
    expect(parsed.searchParams.get('size')).toBe('12')
    expect(parsed.searchParams.get('margin')).toBe('2')
    expect(parsed.searchParams.get('ecc')).toBe('H')
    expect(parsed.searchParams.get('domain')).toBe('go.brand.com')
    expect(parsed.searchParams.get('refresh')).toBe('1')
  })

  it('omits undefined option fields', () => {
    const url = buildQrUrl({
      baseUrl: 'https://api.rerout.co',
      code: 'q4',
      options: { size: 8 },
    })
    expect(url).toBe('https://api.rerout.co/v1/links/q4/qr?size=8')
  })

  it('encodes special code characters', () => {
    const url = buildQrUrl({ baseUrl: 'https://api.rerout.co', code: 'hello world' })
    expect(url).toBe('https://api.rerout.co/v1/links/hello%20world/qr')
  })

  it('strips trailing slashes from baseUrl', () => {
    const url = buildQrUrl({
      baseUrl: 'https://api.rerout.co///',
      code: 'q4',
    })
    expect(url).toBe('https://api.rerout.co/v1/links/q4/qr')
  })

  it('accepts a string refresh override', () => {
    const url = buildQrUrl({
      baseUrl: 'https://api.rerout.co',
      code: 'q4',
      options: { refresh: 'v2' },
    })
    expect(new URL(url).searchParams.get('refresh')).toBe('v2')
  })
})

describe('Rerout.qr', () => {
  it('url() returns the same value as buildQrUrl', () => {
    const r = new Rerout({ apiKey: 'rrk_test', baseUrl: 'https://api.x.dev' })
    expect(r.qr.url('q4', { size: 10 })).toBe(
      buildQrUrl({ baseUrl: 'https://api.x.dev', code: 'q4', options: { size: 10 } }),
    )
  })

  it('svg() fetches the QR endpoint with bearer auth', async () => {
    const fetchImpl = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === 'string' ? input : input.toString()
      expect(url).toBe('https://api.rerout.co/v1/links/q4/qr?size=12')
      const headers = (init?.headers ?? {}) as Record<string, string>
      expect(headers.authorization).toBe('Bearer rrk_test')
      return new Response(JSON.stringify('<svg/>'), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    }) as typeof fetch
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    await r.qr.svg('q4', { size: 12 })
  })
})
