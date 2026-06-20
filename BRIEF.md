# BRIEF: Add tag management to the Rerout SDKs

## Goal

The Rerout API exposes **API-key-authenticated tag management** under the
`/v1/projects/me/tags` namespace (project derived from the key). SDKs already
read tags (a `Link` carries `tags: Tag[]`) but cannot **list / create / update /
delete** them. Add a `tags` namespace to every **core API-client SDK**, mirroring
the existing `links` and `webhooks` namespaces.

The **TypeScript SDK is the golden reference** — match its shape, naming, and
test depth:
- `typescript/src/client.ts` → `Tags` class (`list` / `create` / `update` / `delete`)
- `typescript/src/types.ts` → `Tag`, `TagSummary`, `ListTagsResult`, `CreateTagInput`, `UpdateTagInput`
- `typescript/test/tags.test.ts` → test coverage (mirror it)

## Scope decision (do this first)

- **Core API client** (has a `links` create/list/delete client surface — `go`,
  `python`, `ruby`, `php`, `java`, `kotlin`, `dotnet`, `dart`, `rust`,
  `typescript`): **implement** the `tags` namespace.
- **Framework adapter** (inbound receiver / middleware only — `*-aspnet`,
  `*-spring`, `*-laravel`, `*-django`, `*-rails`): these do not wrap the API
  client. **Do NOT add tag methods.** Report `N/A — framework adapter` and stop.

## API contract

Auth: `Authorization: Bearer <rrk_… api key>` (same as `links`). Project is
resolved from the key — there is **no** project id in the path.

### `GET /v1/projects/me/tags` — list
Response `200`:
```json
{
  "tags": [
    { "id": "tag_abc123", "name": "Spring 2026", "color": "teal", "link_count": 4 }
  ]
}
```
`link_count` is the number of live (non-deleted) links the tag is attached to.
This is the **list-only** shape — `create`/`update` responses omit `link_count`
(see below). Model a `TagSummary` (Tag + `link_count`) for list and a plain
`Tag` (`id`, `name`, `color`) for create/update.

### `POST /v1/projects/me/tags` — create
Request body:
```json
{
  "name": "Spring 2026",   // required, string
  "color": "teal"          // optional, string; server validates + defaults to "teal"
}
```
Response `201`: a `Tag` — `{ "id", "name", "color" }`.

### `PATCH /v1/projects/me/tags/:tag_id` — update
Request body (both optional; omitted = unchanged):
```json
{ "name": "Renamed", "color": "red" }
```
Response `200`: the updated `Tag`. Mirror your SDK's existing `links.update`
convention for "leave field alone vs change it". **Do not** add a client-side
empty-payload check unless your `links.update` already has one (the TS reference
does not — the server returns `400` for a fully empty patch).

### `DELETE /v1/projects/me/tags/:tag_id` — delete
Response `200`: `{ "deleted": true }`. Deleting also drops the tag's link
assignments server-side.

`tag_id` and any path segment must be URL-encoded exactly as `links`/`webhooks` do.

## Per-language tasks

1. Add the tag types (`Tag` likely already exists for `Link.tags` — reuse it;
   add `TagSummary`/`link_count`, `CreateTagInput`, `UpdateTagInput`,
   `ListTagsResult` or the idiomatic equivalents).
2. Add a `tags` namespace/accessor on the client wired the same way as
   `links`/`webhooks`.
3. Implement `list`, `create`, `update(tagId, input)`, `delete(tagId)`.
4. Export the new public types/classes from the package entry point.
5. Add tests mirroring `typescript/test/tags.test.ts` using the SDK's existing
   HTTP-stub/mock pattern (no real network). Cover: list parses `link_count`,
   create sends name+color, update PATCHes by id and forwards only given fields,
   delete sends DELETE and returns the result.
6. Update the package README's usage/namespace list if it enumerates namespaces.

## Done criteria

- New tag tests pass alongside the existing suite (run the package's test command).
- Lint/format/typecheck clean per the package's tooling.
- No version bump unless the package's existing convention requires it for an
  unreleased change (most are at an unreleased `0.x`; leave as-is).
- Report: files changed, test command + result, and anything that diverged from
  this brief (and why).
