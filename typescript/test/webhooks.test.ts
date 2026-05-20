import { createHmac } from 'node:crypto'
import { describe, expect, it } from 'vitest'
import { verifyReroutSignature, DEFAULT_SIGNATURE_TOLERANCE_SECONDS } from '../src/index.js'

const SECRET = 'whsec_super_secret_value'
const RAW_BODY = JSON.stringify({
  id: 'evt_abc',
  type: 'link.clicked',
  data: { code: 'q4' },
})

function signedHeader(opts: { ts: number; secret?: string; body?: string }): string {
  const body = opts.body ?? RAW_BODY
  const secret = opts.secret ?? SECRET
  const hmac = createHmac('sha256', secret).update(`${opts.ts}.${body}`).digest('hex')
  return `t=${opts.ts},v1=${hmac}`
}

describe('verifyReroutSignature', () => {
  it('accepts a freshly signed payload', () => {
    const ts = 1_700_000_000
    const header = signedHeader({ ts })
    expect(
      verifyReroutSignature({
        rawBody: RAW_BODY,
        signatureHeader: header,
        secret: SECRET,
        now: () => ts,
      }),
    ).toBe(true)
  })

  it('rejects when the HMAC is wrong', () => {
    const ts = 1_700_000_000
    const header = signedHeader({ ts, secret: 'whsec_different' })
    expect(
      verifyReroutSignature({
        rawBody: RAW_BODY,
        signatureHeader: header,
        secret: SECRET,
        now: () => ts,
      }),
    ).toBe(false)
  })

  it('rejects when the body has been tampered with', () => {
    const ts = 1_700_000_000
    const header = signedHeader({ ts })
    expect(
      verifyReroutSignature({
        rawBody: `${RAW_BODY} `, // extra trailing space
        signatureHeader: header,
        secret: SECRET,
        now: () => ts,
      }),
    ).toBe(false)
  })

  it('rejects expired payloads outside the tolerance window', () => {
    const signed = 1_700_000_000
    const header = signedHeader({ ts: signed })
    expect(
      verifyReroutSignature({
        rawBody: RAW_BODY,
        signatureHeader: header,
        secret: SECRET,
        now: () => signed + DEFAULT_SIGNATURE_TOLERANCE_SECONDS + 1,
      }),
    ).toBe(false)
  })

  it('accepts payloads exactly at the tolerance boundary', () => {
    const signed = 1_700_000_000
    const header = signedHeader({ ts: signed })
    expect(
      verifyReroutSignature({
        rawBody: RAW_BODY,
        signatureHeader: header,
        secret: SECRET,
        now: () => signed + DEFAULT_SIGNATURE_TOLERANCE_SECONDS,
      }),
    ).toBe(true)
  })

  it('disables the timestamp check when toleranceSeconds=0', () => {
    const signed = 1_700_000_000
    const header = signedHeader({ ts: signed })
    expect(
      verifyReroutSignature({
        rawBody: RAW_BODY,
        signatureHeader: header,
        secret: SECRET,
        toleranceSeconds: 0,
        now: () => signed + 10_000_000,
      }),
    ).toBe(true)
  })

  it('rejects malformed headers', () => {
    const cases = [
      '',
      'garbage',
      't=,v1=abc',
      'v1=abc', // missing t
      't=1700000000', // missing v1
      't=notanumber,v1=abc',
      't=-1,v1=abc',
      't=1700000000,v1=nothex',
      't=1700000000,v1=12345', // odd length
    ]
    for (const header of cases) {
      expect(
        verifyReroutSignature({
          rawBody: RAW_BODY,
          signatureHeader: header,
          secret: SECRET,
          now: () => 1_700_000_000,
        }),
        `should reject ${header}`,
      ).toBe(false)
    }
  })

  it('handles header key casing variations', () => {
    const ts = 1_700_000_000
    const hmac = createHmac('sha256', SECRET).update(`${ts}.${RAW_BODY}`).digest('hex')
    expect(
      verifyReroutSignature({
        rawBody: RAW_BODY,
        signatureHeader: `T=${ts}, V1=${hmac}`,
        secret: SECRET,
        now: () => ts,
      }),
    ).toBe(true)
  })

  it('rejects when secret is empty', () => {
    const ts = 1_700_000_000
    const header = signedHeader({ ts })
    expect(
      verifyReroutSignature({
        rawBody: RAW_BODY,
        signatureHeader: header,
        secret: '',
        now: () => ts,
      }),
    ).toBe(false)
  })

  it('rejects when signatureHeader is missing', () => {
    expect(
      verifyReroutSignature({
        rawBody: RAW_BODY,
        signatureHeader: '',
        secret: SECRET,
        now: () => 1_700_000_000,
      }),
    ).toBe(false)
  })

  it('rejects when rawBody is undefined', () => {
    const ts = 1_700_000_000
    const header = signedHeader({ ts })
    expect(
      verifyReroutSignature({
        rawBody: undefined as unknown as string,
        signatureHeader: header,
        secret: SECRET,
        now: () => ts,
      }),
    ).toBe(false)
  })
})
