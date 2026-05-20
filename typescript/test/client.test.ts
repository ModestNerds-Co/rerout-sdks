import { describe, expect, it, vi } from 'vitest'
import { Rerout, ReroutError } from '../src/index.js'

function fakeFetch(
  responses: Array<
    | { status: number; body: unknown; headers?: Record<string, string> }
    | (() => never)
  >,
): {
  fetch: typeof fetch
  calls: { url: string; init: RequestInit }[]
} {
  const calls: { url: string; init: RequestInit }[] = []
  let i = 0
  const fn: typeof fetch = vi.fn(async (input, init) => {
    const url = typeof input === 'string' ? input : input.toString()
    calls.push({ url, init: init ?? {} })
    const entry = responses[i++]
    if (entry === undefined) {
      throw new Error(`fakeFetch ran out of responses for ${url}`)
    }
    if (typeof entry === 'function') {
      entry()
      throw new Error('unreachable')
    }
    const headers = new Headers({ 'content-type': 'application/json', ...entry.headers })
    return new Response(
      typeof entry.body === 'string' ? entry.body : JSON.stringify(entry.body),
      { status: entry.status, headers },
    )
  })
  return { fetch: fn, calls }
}

describe('Rerout constructor', () => {
  it('requires an API key', () => {
    expect(() => new Rerout({ apiKey: '' })).toThrow(ReroutError)
    expect(() => new Rerout({ apiKey: undefined as unknown as string })).toThrow(
      ReroutError,
    )
  })

  it('defaults baseUrl to production and trims trailing slashes', () => {
    const r1 = new Rerout({ apiKey: 'rrk_test' })
    expect(r1.resolvedBaseUrl).toBe('https://api.rerout.co')
    const r2 = new Rerout({ apiKey: 'rrk_test', baseUrl: 'https://api.x.dev/' })
    expect(r2.resolvedBaseUrl).toBe('https://api.x.dev')
    const r3 = new Rerout({ apiKey: 'rrk_test', baseUrl: 'https://api.x.dev///' })
    expect(r3.resolvedBaseUrl).toBe('https://api.x.dev')
  })

  it('exposes the three namespaces', () => {
    const r = new Rerout({ apiKey: 'rrk_test' })
    expect(r.links).toBeDefined()
    expect(r.project).toBeDefined()
    expect(r.qr).toBeDefined()
  })
})

describe('request transport', () => {
  it('sends Authorization, accept, and content-type headers', async () => {
    const fake = fakeFetch([{ status: 200, body: { code: 'x', short_url: 'https://rerout.co/x' } }])
    const r = new Rerout({
      apiKey: 'rrk_secret',
      fetch: fake.fetch,
      defaultHeaders: { 'user-agent': 'rerout-sdk-test/0.1' },
    })
    await r.links.create({ target_url: 'https://example.com' })
    const call = fake.calls[0]!
    const headers = call.init.headers as Record<string, string>
    expect(headers.authorization).toBe('Bearer rrk_secret')
    expect(headers.accept).toBe('application/json')
    expect(headers['content-type']).toBe('application/json')
    expect(headers['user-agent']).toBe('rerout-sdk-test/0.1')
  })

  it('serializes the body for POST/PATCH', async () => {
    const fake = fakeFetch([{ status: 200, body: { code: 'x' } }])
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fake.fetch })
    await r.links.create({ target_url: 'https://example.com' })
    const body = fake.calls[0]!.init.body
    expect(body).toBe(JSON.stringify({ target_url: 'https://example.com' }))
  })

  it('serializes numeric query params', async () => {
    const fake = fakeFetch([
      { status: 200, body: { code: 'abc', days: 7, total_clicks: 0, qr_scans: 0, countries: [], referrers: [] } },
    ])
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fake.fetch })
    await r.links.stats('abc', 7)
    expect(fake.calls[0]!.url).toMatch(/\/v1\/links\/abc\/stats\?days=7$/)
  })

  it('rejects with ReroutError carrying server code/message on 4xx', async () => {
    const fake = fakeFetch([
      { status: 400, body: { code: 'bad_target_url', message: 'target_url must use https.' } },
    ])
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fake.fetch })
    await expect(r.links.create({ target_url: 'http://insecure.example' })).rejects.toMatchObject({
      name: 'ReroutError',
      code: 'bad_target_url',
      status: 400,
      message: 'target_url must use https.',
    })
  })

  it('synthesizes codes for status-only errors', async () => {
    const fake = fakeFetch([
      { status: 429, body: '' },
      { status: 401, body: '' },
      { status: 500, body: '' },
    ])
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fake.fetch })
    await expect(r.links.list()).rejects.toMatchObject({ code: 'rate_limited', status: 429 })
    await expect(r.links.list()).rejects.toMatchObject({ code: 'unauthorized', status: 401 })
    await expect(r.links.list()).rejects.toMatchObject({ code: 'server_error', status: 500 })
  })

  it('reports network errors with code=network_error', async () => {
    const fetchImpl: typeof fetch = vi.fn(async () => {
      throw new Error('socket hang up')
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    await expect(r.links.list()).rejects.toMatchObject({
      code: 'network_error',
      status: 0,
      message: 'socket hang up',
    })
  })

  it('reports timeouts with code=timeout', async () => {
    const fetchImpl: typeof fetch = vi.fn(async (_input, init) => {
      const signal = init?.signal
      if (signal) {
        await new Promise((_resolve, reject) => {
          if (signal.aborted) reject(new Error('AbortError'))
          else signal.addEventListener('abort', () => reject(new Error('AbortError')))
        })
      }
      throw new Error('unreachable')
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl, timeoutMs: 5 })
    await expect(r.links.list()).rejects.toMatchObject({ code: 'timeout', status: 0 })
  })

  it('throws unexpected_response on a non-JSON 200 body', async () => {
    const fake = fakeFetch([{ status: 200, body: '<not-json>', headers: { 'content-type': 'text/plain' } }])
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fake.fetch })
    await expect(r.links.list()).rejects.toMatchObject({
      code: 'unexpected_response',
      status: 200,
    })
  })

  it('returns undefined for empty 2xx bodies', async () => {
    const fake = fakeFetch([{ status: 200, body: '' }])
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fake.fetch })
    const result = await r.links.delete('abc')
    expect(result).toBeUndefined()
  })

  it('isRateLimited / isServerError reflect status', () => {
    const err = new ReroutError({ code: 'x', message: 'x', status: 429 })
    expect(err.isRateLimited).toBe(true)
    expect(err.isServerError).toBe(false)
    const err2 = new ReroutError({ code: 'y', message: 'y', status: 503 })
    expect(err2.isServerError).toBe(true)
  })
})
