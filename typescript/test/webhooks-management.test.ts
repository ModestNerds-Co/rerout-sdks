import { describe, expect, it, vi } from 'vitest'
import { Rerout } from '../src/index.js'
import type { CreatedWebhook, Webhook } from '../src/index.js'

function makeFetch(handler: (url: string, init: RequestInit) => Response | Promise<Response>) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input.toString()
    return handler(url, init ?? {})
  }) as typeof fetch
}

const SAMPLE_WEBHOOK: Webhook = {
  id: 'wh_abc123',
  project_id: 'prj_test',
  name: 'Order events',
  url: 'https://example.com/hooks/rerout',
  events: ['link.created', 'link.clicked'],
  is_active: true,
  payload_format: 'json',
  created_at: 1_700_000_000,
  updated_at: 1_700_000_000,
  last_delivery_at: null,
  last_success_at: null,
  last_failure_at: null,
}

describe('Webhooks', () => {
  it('create posts to /v1/projects/me/webhooks and returns the signing secret', async () => {
    const created: CreatedWebhook = {
      endpoint: SAMPLE_WEBHOOK,
      signing_secret: 'whsec_supersecret',
    }
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/projects/me/webhooks')
      expect(init.method).toBe('POST')
      const body = JSON.parse(init.body as string)
      expect(body).toEqual({
        name: 'Order events',
        url: 'https://example.com/hooks/rerout',
        events: ['link.created', 'link.clicked'],
      })
      return new Response(JSON.stringify(created), {
        status: 201,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.webhooks.create({
      name: 'Order events',
      url: 'https://example.com/hooks/rerout',
      events: ['link.created', 'link.clicked'],
    })
    expect(result.signing_secret).toBe('whsec_supersecret')
    expect(result.endpoint.id).toBe('wh_abc123')
  })

  it('list returns endpoints and supported event types', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/projects/me/webhooks')
      expect(init.method).toBe('GET')
      return new Response(
        JSON.stringify({
          endpoints: [SAMPLE_WEBHOOK],
          event_types: ['link.created', 'link.clicked', 'domain.verified'],
        }),
        { status: 200, headers: { 'content-type': 'application/json' } },
      )
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.webhooks.list()
    expect(result.endpoints).toHaveLength(1)
    expect(result.endpoints[0]?.url).toBe('https://example.com/hooks/rerout')
    expect(result.event_types).toContain('domain.verified')
  })

  it('delete sends DELETE and encodes the endpoint id', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/projects/me/webhooks/wh_abc123')
      expect(init.method).toBe('DELETE')
      return new Response(JSON.stringify({ deleted: true }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    expect(await r.webhooks.delete('wh_abc123')).toEqual({ deleted: true })
  })

  it('create forwards is_active and payload_format when provided', async () => {
    const fetchImpl = makeFetch((url, init) => {
      const body = JSON.parse(init.body as string)
      expect(body.is_active).toBe(false)
      expect(body.payload_format).toBe('slack')
      return new Response(
        JSON.stringify({
          endpoint: { ...SAMPLE_WEBHOOK, is_active: false, payload_format: 'slack' },
          signing_secret: 'whsec_x',
        }),
        { status: 201, headers: { 'content-type': 'application/json' } },
      )
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.webhooks.create({
      name: 'Slack',
      url: 'https://hooks.slack.com/services/T/B/x',
      events: ['link.created'],
      is_active: false,
      payload_format: 'slack',
    })
    expect(result.endpoint.payload_format).toBe('slack')
  })
})
