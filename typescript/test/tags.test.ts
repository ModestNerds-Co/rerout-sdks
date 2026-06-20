import { describe, expect, it, vi } from 'vitest'
import { Rerout } from '../src/index.js'
import type { Tag, TagSummary } from '../src/index.js'

function makeFetch(handler: (url: string, init: RequestInit) => Response | Promise<Response>) {
  return vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input.toString()
    return handler(url, init ?? {})
  }) as typeof fetch
}

const SAMPLE_TAG: Tag = { id: 'tag_abc123', name: 'Spring 2026', color: 'teal' }
const SAMPLE_SUMMARY: TagSummary = { ...SAMPLE_TAG, link_count: 4 }

describe('Tags', () => {
  it('list GETs /v1/projects/me/tags and returns link counts', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/projects/me/tags')
      expect(init.method).toBe('GET')
      return new Response(JSON.stringify({ tags: [SAMPLE_SUMMARY] }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const result = await r.tags.list()
    expect(result.tags).toHaveLength(1)
    expect(result.tags[0]?.link_count).toBe(4)
  })

  it('create POSTs the name and color', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/projects/me/tags')
      expect(init.method).toBe('POST')
      expect(JSON.parse(init.body as string)).toEqual({ name: 'Spring 2026', color: 'teal' })
      return new Response(JSON.stringify(SAMPLE_TAG), {
        status: 201,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const tag = await r.tags.create({ name: 'Spring 2026', color: 'teal' })
    expect(tag.id).toBe('tag_abc123')
  })

  it('update PATCHes the tag by id and encodes it', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/projects/me/tags/tag_abc123')
      expect(init.method).toBe('PATCH')
      expect(JSON.parse(init.body as string)).toEqual({ color: 'red' })
      return new Response(JSON.stringify({ ...SAMPLE_TAG, color: 'red' }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const tag = await r.tags.update('tag_abc123', { color: 'red' })
    expect(tag.color).toBe('red')
  })

  it('update forwards only the provided fields', async () => {
    const fetchImpl = makeFetch((_url, init) => {
      expect(JSON.parse(init.body as string)).toEqual({ name: 'Renamed' })
      return new Response(JSON.stringify({ ...SAMPLE_TAG, name: 'Renamed' }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    const tag = await r.tags.update('tag_abc123', { name: 'Renamed' })
    expect(tag.name).toBe('Renamed')
  })

  it('delete sends DELETE and returns the result', async () => {
    const fetchImpl = makeFetch((url, init) => {
      expect(url).toBe('https://api.rerout.co/v1/projects/me/tags/tag_abc123')
      expect(init.method).toBe('DELETE')
      return new Response(JSON.stringify({ deleted: true }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    })
    const r = new Rerout({ apiKey: 'rrk_test', fetch: fetchImpl })
    expect(await r.tags.delete('tag_abc123')).toEqual({ deleted: true })
  })
})
