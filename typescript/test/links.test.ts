import { describe, expect, it, vi } from 'vitest'
import { Rerout } from '../src/index.js'
import type { Link } from '../src/index.js'

function makeFetch(handler: (url: string, init: RequestInit) => Response | Promise<Response>) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input.toString()
    return handler(url, init ?? {})
  }) as typeof fetch
}

const SAMPLE_LINK: Link = {
  code: 'abc123',
  short_url: 'https://rerout.co/abc123',
  domain_hostname: null,
  target_url: 'https://example.com',
  project_id: 'prj_test',
  expires_at: null,
  is_active: true,
  seo_title: null,
  seo_description: null,
  seo_image_url: null,
  seo_canonical_url: null,
  seo_noindex: true,
  seo_updated_at: null,
  created_at: 1_700_000_000,
  updated_at: 1_700_000_000,
}

describe('Links', () => {
  it('create posts to /v1/links and returns Link', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/links')
      expect(init.method).toBe('POST')
      const body = JSON.parse(init.body as string)
      expect(body.target_url).toBe('https://example.com')
      return new Response(JSON.stringify(SAMPLE_LINK), {
        status: 201,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const link = await r.links.create({ target_url: 'https://example.com' })
    expect(link).toEqual(SAMPLE_LINK)
  })

  it('list passes cursor + limit as query params', async () => {
    const fetchImpl = makeFetch((url) => {
      expect(url).toBe('https://api.rerout.co/v1/links?cursor=42&limit=25')
      return new Response(JSON.stringify({ links: [], next_cursor: null }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.links.list({ cursor: 42, limit: 25 })
    expect(result.links).toEqual([])
    expect(result.next_cursor).toBeNull()
  })

  it('get encodes the code', async () => {
    const fetchImpl = makeFetch((url) => {
      // Code "go/promo" must be encoded; the slash is escaped to %2F.
      expect(url).toBe('https://api.rerout.co/v1/links/go%2Fpromo')
      return new Response(JSON.stringify(SAMPLE_LINK), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    await r.links.get('go/promo')
  })

  it('update patches with only provided fields', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/links/abc123')
      expect(init.method).toBe('PATCH')
      expect(JSON.parse(init.body as string)).toEqual({ is_active: false })
      return new Response(JSON.stringify({ ...SAMPLE_LINK, is_active: false }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.links.update('abc123', { is_active: false })
    expect(result.is_active).toBe(false)
  })

  it('delete sends DELETE method', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/links/abc123')
      expect(init.method).toBe('DELETE')
      return new Response(JSON.stringify({ deleted: true }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    expect(await r.links.delete('abc123')).toEqual({ deleted: true })
  })

  it('stats defaults days=30', async () => {
    const fetchImpl = makeFetch((url) => {
      expect(url).toContain('days=30')
      return new Response(
        JSON.stringify({
          code: 'abc123',
          days: 30,
          total_clicks: 42,
          qr_scans: 5,
          countries: [],
          referrers: [],
        }),
        { status: 200, headers: { 'content-type': 'application/json' } },
      )
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.links.stats('abc123')
    expect(result.total_clicks).toBe(42)
    expect(result.qr_scans).toBe(5)
  })

  it('encodes codes with spaces, +, and unicode', async () => {
    const seen: string[] = []
    const fetchImpl = makeFetch((url) => {
      seen.push(url)
      return new Response(JSON.stringify(SAMPLE_LINK), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    await r.links.get('hello world')
    await r.links.get('a+b')
    await r.links.get('café')
    expect(seen).toEqual([
      'https://api.rerout.co/v1/links/hello%20world',
      'https://api.rerout.co/v1/links/a%2Bb',
      'https://api.rerout.co/v1/links/caf%C3%A9',
    ])
  })
})
