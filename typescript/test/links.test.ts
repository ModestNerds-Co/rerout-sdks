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
  tags: [{ id: 'tag_1', name: 'campaign', color: '#ff8800' }],
  password_protected: false,
  max_clicks: null,
  click_count: 0,
  track_conversions: false,
  routing_rules: [],
  ab_variants: [],
  created_at: 1_700_000_000,
  updated_at: 1_700_000_000,
}

describe('Links', () => {
  it('create posts to /v1/links and returns Link with empty tags', async () => {
    // The API returns an empty `tags` array on create.
    const created: Link = { ...SAMPLE_LINK, tags: [] }
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/links')
      expect(init.method).toBe('POST')
      const body = JSON.parse(init.body as string)
      expect(body.target_url).toBe('https://example.com')
      return new Response(JSON.stringify(created), {
        status: 201,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const link = await r.links.create({ target_url: 'https://example.com' })
    expect(link).toEqual(created)
    expect(link.tags).toEqual([])
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
    const link = await r.links.get('go/promo')
    expect(link.tags).toEqual([{ id: 'tag_1', name: 'campaign', color: '#ff8800' }])
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

  it('create forwards Smart Links fields when provided', async () => {
    const created: Link = {
      ...SAMPLE_LINK,
      tags: [],
      password_protected: true,
      max_clicks: 100,
      track_conversions: true,
      routing_rules: [
        {
          condition_type: 'country',
          condition_op: 'is',
          condition_value: 'ZA',
          target_url: 'https://example.com/za',
        },
      ],
      ab_variants: [
        { id: 1, target_url: 'https://example.com/a', weight: 1 },
        { id: 2, target_url: 'https://example.com/b', weight: 3 },
      ],
    }
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/links')
      const body = JSON.parse(init.body as string)
      expect(body.password).toBe('s3cret')
      expect(body.max_clicks).toBe(100)
      expect(body.track_conversions).toBe(true)
      expect(body.routing_rules).toEqual([
        {
          condition_type: 'country',
          condition_op: 'is',
          condition_value: 'ZA',
          target_url: 'https://example.com/za',
        },
      ])
      expect(body.ab_variants).toEqual([
        { target_url: 'https://example.com/a' },
        { target_url: 'https://example.com/b', weight: 3 },
      ])
      return new Response(JSON.stringify(created), {
        status: 201,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const link = await r.links.create({
      target_url: 'https://example.com',
      password: 's3cret',
      max_clicks: 100,
      track_conversions: true,
      routing_rules: [
        {
          condition_type: 'country',
          condition_op: 'is',
          condition_value: 'ZA',
          target_url: 'https://example.com/za',
        },
      ],
      ab_variants: [
        { target_url: 'https://example.com/a' },
        { target_url: 'https://example.com/b', weight: 3 },
      ],
    })
    expect(link.password_protected).toBe(true)
    expect(link.max_clicks).toBe(100)
    expect(link.track_conversions).toBe(true)
    expect(link.routing_rules[0]?.condition_value).toBe('ZA')
    expect(link.ab_variants[1]?.weight).toBe(3)
  })

  it('update clears password and max_clicks with explicit null', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/links/abc123')
      expect(init.method).toBe('PATCH')
      expect(JSON.parse(init.body as string)).toEqual({
        password: null,
        max_clicks: null,
        track_conversions: false,
      })
      return new Response(JSON.stringify(SAMPLE_LINK), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    await r.links.update('abc123', {
      password: null,
      max_clicks: null,
      track_conversions: false,
    })
  })

  it('update full-replaces routing_rules and ab_variants', async () => {
    const fetchImpl = makeFetch((url, init) => {
      const body = JSON.parse(init.body as string)
      expect(body.routing_rules).toEqual([
        {
          condition_type: 'device',
          condition_op: 'in',
          condition_value: 'mobile,tablet',
          target_url: 'https://example.com/m',
        },
      ])
      expect(body.ab_variants).toEqual([{ target_url: 'https://example.com/x', weight: 2 }])
      return new Response(JSON.stringify(SAMPLE_LINK), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    await r.links.update('abc123', {
      routing_rules: [
        {
          condition_type: 'device',
          condition_op: 'in',
          condition_value: 'mobile,tablet',
          target_url: 'https://example.com/m',
        },
      ],
      ab_variants: [{ target_url: 'https://example.com/x', weight: 2 }],
    })
  })

  it('createBatch posts to /v1/links/batch and returns per-item results', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/links/batch')
      expect(init.method).toBe('POST')
      expect(JSON.parse(init.body as string)).toEqual({
        links: [
          { target_url: 'https://example.com/1' },
          { target_url: 'https://example.com/2', code: 'two' },
        ],
      })
      return new Response(
        JSON.stringify({
          created: 1,
          total: 2,
          results: [
            { index: 0, ok: true, code: 'aaa111' },
            { index: 1, ok: false, error: 'code already in use' },
          ],
        }),
        { status: 207, headers: { 'content-type': 'application/json' } },
      )
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.links.createBatch([
      { target_url: 'https://example.com/1' },
      { target_url: 'https://example.com/2', code: 'two' },
    ])
    expect(result.created).toBe(1)
    expect(result.total).toBe(2)
    expect(result.results[0]?.code).toBe('aaa111')
    expect(result.results[1]?.ok).toBe(false)
    expect(result.results[1]?.error).toBe('code already in use')
  })
})
