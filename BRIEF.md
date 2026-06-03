# BRIEF: Add webhook endpoint management to the Rerout SDKs

## Goal

The Rerout API now exposes **API-key-authenticated webhook endpoint management**
under the `/v1/projects/me/webhooks` namespace (project derived from the key).
Until now SDKs only *verified* inbound webhook signatures; they could not
**create / list / delete** endpoints. Add that management surface to every
**core API-client SDK**, mirroring the existing `links` CRUD namespace.

The TypeScript SDK is the **golden reference** — match its shape, naming, and
test depth:
- `typescript/src/client.ts` → `Webhooks` class (`create` / `list` / `delete`)
- `typescript/src/types.ts` → `Webhook`, `CreateWebhookInput`, `CreatedWebhook`, `ListWebhooksResult`
- `typescript/test/webhooks-management.test.ts` → test coverage

## Scope decision (do this first)

Determine what kind of package you're in:
- **Core API client** (has a `links` create/list/delete client surface, e.g.
  `go`, `python`, `ruby`, `php`, `java`, `kotlin`, `dotnet`, `dart`, `rust`):
  **implement** webhook management mirroring the `links` namespace.
- **Framework adapter** (inbound webhook receiver / middleware only — e.g.
  `*-aspnet`, `*-spring`, `*-laravel`, `*-django`, `*-rails`): these handle
  *receiving* webhooks, not managing them. **Do NOT add management methods.**
  Report `N/A — framework adapter` and stop.

## API contract

Auth: `Authorization: Bearer <rrk_… api key>` (same as `links`). Project is
resolved from the key — there is **no** project id in the path.

### `POST /v1/projects/me/webhooks` — create
Request body:
```json
{
  "name": "Order events",                         // required, string
  "url": "https://example.com/hooks/rerout",      // required, public https URL
  "events": ["link.created", "link.clicked"],     // required, non-empty string[]
  "is_active": true,                              // optional, bool, default true
  "payload_format": "json"                        // optional, "json" | "slack"
}
```
Response `201`:
```json
{
  "endpoint": { /* Webhook, see below */ },
  "signing_secret": "whsec_…"   // returned ONCE — surface it to the caller
}
```

### `GET /v1/projects/me/webhooks` — list
Response `200`:
```json
{
  "endpoints": [ /* Webhook[] */ ],
  "event_types": ["link.created", "..."]   // every event the server can send
}
```

### `DELETE /v1/projects/me/webhooks/{endpoint_id}` — delete
Response `200`: `{ "deleted": true }`. URL-encode `endpoint_id` (ids look like
`wh_…`). Idempotent.

### `Webhook` shape (mirrors server `WebhookEndpointResponse`)
```
id              string
project_id      string
name            string
url             string
events          string[]
is_active       bool
payload_format  string
created_at      int (unix seconds)
updated_at      int (unix seconds)
last_delivery_at  int | null
last_success_at   int | null
last_failure_at   int | null
```

### Valid event types (for docs/reference, do not hard-validate client-side)
```
link.created link.updated link.deleted link.clicked
domain.added domain.verified domain.failed domain.removed
api_key.created api_key.revoked
qr.generated qr.regenerated qr.scanned
qr_branding.updated qr_branding.reset
qr_preset.created qr_preset.applied qr_preset.deleted
project.created project.updated
```

## Implementation rules

1. **Mirror the `links` namespace** in this SDK exactly — same client plumbing,
   same request helper, same error handling, same module/file layout, same
   naming idioms (e.g. Go `Webhooks` with `Create/List/Delete`, Python
   `client.webhooks.create(...)`, Ruby `client.webhooks.create(...)`).
2. Method names: **create**, **list**, **delete** (use the language's idiomatic
   casing). Expose them as a `webhooks` namespace on the main client, alongside
   `links` / `project` / `qr`.
3. `create` returns the wrapped `{ endpoint, signing_secret }` faithfully — do
   not flatten or drop `signing_secret`.
4. Add request/response types/models matching the shapes above, following how
   this SDK already models `Link` / `CreateLinkInput`.
5. Do not break, rename, or move the existing inbound **signature verification**
   code — that stays as-is.
6. Export the new public types/classes from the SDK's public surface (mirror how
   `Link` etc. are exported).

## Tests (required — match the reference's depth)

Add tests in this SDK's existing test framework/location, mirroring
`links` tests. Cover, with a mocked HTTP layer (do NOT hit the network):
- `create` → POSTs to `/v1/projects/me/webhooks`, sends the right body,
  returns `signing_secret` + `endpoint`.
- `list` → GETs `/v1/projects/me/webhooks`, parses `endpoints` + `event_types`.
- `delete` → DELETEs `/v1/projects/me/webhooks/{id}`, returns `{ deleted: true }`.
- `create` forwards optional `is_active` / `payload_format` when provided.

## Done criteria

- Webhook management namespace implemented + exported.
- Tests added and **the SDK's full test suite passes** (run it).
- Existing signature-verification code untouched and still passing.
- If framework adapter: no code change, report `N/A`.

Report back: files changed, the exact test command you ran, and its pass/fail
result.
