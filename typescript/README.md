# @rerout/sdk

Official TypeScript / JavaScript SDK for the [Rerout](https://rerout.co) API.

## Install

```bash
npm install @rerout/sdk
# or
pnpm add @rerout/sdk
# or
bun add @rerout/sdk
```

Requires Node 18+ (uses the global `fetch` and `AbortController`). Works in
modern bundlers, Bun, Deno, and Cloudflare Workers — pass a custom `fetch` in
edge runtimes if needed.

## Usage

```ts
import { Rerout } from '@rerout/sdk'

const rerout = new Rerout({ apiKey: process.env.REROUT_API_KEY! })

const link = await rerout.links.create({
  target_url: 'https://example.com/q4-sale',
  domain_hostname: 'go.brand.com',
  code: 'q4',
})

console.log(link.short_url) // https://go.brand.com/q4

const stats = await rerout.project.stats(7)
console.log(`Last 7 days: ${stats.total_clicks} clicks, ${stats.qr_scans} QR scans`)
```

## API

### Construction

```ts
const rerout = new Rerout({
  apiKey: 'rrk_…',                // required
  baseUrl: 'https://api.rerout.co', // optional, defaults shown
  timeoutMs: 30_000,                // optional
  fetch: customFetch,               // optional — inject your own fetch
  defaultHeaders: {                 // optional — added to every request
    'user-agent': 'my-app/1.0',
  },
})
```

### Links

```ts
rerout.links.create({ target_url, domain_hostname?, code?, expires_at?, ...seo })
rerout.links.list({ cursor?, limit? })
rerout.links.get(code)
rerout.links.update(code, { target_url?, expires_at?, is_active?, ...seo })
rerout.links.delete(code)
rerout.links.stats(code, days = 30)
```

Every returned `Link` includes a read-only `tags` array of `{ id, name, color }`
objects (empty on create; populated on get/list/update). Tag writes aren't
supported for API-key clients.

### Project

```ts
rerout.project.stats(days = 30)
rerout.project.me()
```

### QR codes

```ts
rerout.qr.url(code, { size?, margin?, ecc?, domain?, refresh? }) // returns string
await rerout.qr.svg(code, opts) // fetches the rendered SVG
```

### Webhook signature verification

```ts
import { verifyReroutSignature } from '@rerout/sdk'

const ok = verifyReroutSignature({
  rawBody,
  signatureHeader: req.headers['x-rerout-signature']!,
  secret: process.env.REROUT_WEBHOOK_SECRET!,
})
```

Defaults to a 5-minute timestamp tolerance; pass `toleranceSeconds: 0` to
disable that check.

## Error handling

Every method throws `ReroutError` on failure:

```ts
import { ReroutError } from '@rerout/sdk'

try {
  await rerout.links.create({ target_url: 'http://insecure.example' })
} catch (err) {
  if (err instanceof ReroutError) {
    console.error(err.code)    // 'bad_target_url'
    console.error(err.status)  // 400
    console.error(err.message) // 'target_url must use https.'
    if (err.isRateLimited) { /* back off */ }
  }
}
```

Synthetic codes when the server didn't return a JSON body:
`network_error`, `timeout`, `unexpected_response`, `unauthorized`,
`forbidden`, `not_found`, `rate_limited`, `server_error`, `client_error`.

## Local development

```bash
npm install
npm run typecheck
npm run test
npm run build
```

## License

MIT — see [LICENSE](../LICENSE).
