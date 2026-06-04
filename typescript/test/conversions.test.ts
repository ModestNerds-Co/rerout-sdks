import { describe, expect, it, vi } from 'vitest'
import { Rerout } from '../src/index.js'

function makeFetch(handler: (url: string, init: RequestInit) => Response | Promise<Response>) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input.toString()
    return handler(url, init ?? {})
  }) as typeof fetch
}

describe('Conversions', () => {
  it('record posts to /v1/conversions with the click id and event name', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/conversions')
      expect(init.method).toBe('POST')
      expect(JSON.parse(init.body as string)).toEqual({
        click_id: 'rrid_abc',
        event_name: 'purchase',
      })
      return new Response(JSON.stringify({ recorded: true, duplicate: false }), {
        status: 201,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.conversions.record({
      click_id: 'rrid_abc',
      event_name: 'purchase',
    })
    expect(result.recorded).toBe(true)
    expect(result.duplicate).toBe(false)
  })

  it('record forwards value_cents and currency when provided', async () => {
    const fetchImpl = makeFetch((url, init) => {
      const body = JSON.parse(init.body as string)
      expect(body.value_cents).toBe(4999)
      expect(body.currency).toBe('USD')
      return new Response(JSON.stringify({ recorded: true, duplicate: false }), {
        status: 201,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    await r.conversions.record({
      click_id: 'rrid_abc',
      event_name: 'purchase',
      value_cents: 4999,
      currency: 'USD',
    })
  })

  it('record surfaces duplicate=true for an already-recorded conversion', async () => {
    const fetchImpl = makeFetch(() => {
      return new Response(JSON.stringify({ recorded: true, duplicate: true }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.conversions.record({
      click_id: 'rrid_abc',
      event_name: 'purchase',
    })
    expect(result.duplicate).toBe(true)
  })

  it('record surfaces server error code + message', async () => {
    const fetchImpl = makeFetch(() => {
      return new Response(
        JSON.stringify({ code: 'bad_request', message: 'click_id is required.' }),
        { status: 400, headers: { 'content-type': 'application/json' } },
      )
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    await expect(
      r.conversions.record({ click_id: '', event_name: 'purchase' }),
    ).rejects.toMatchObject({ code: 'bad_request', status: 400 })
  })
})
