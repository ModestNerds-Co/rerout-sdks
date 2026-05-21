# Rerout SDK — shared brief for language ports

This brief is the source of truth for every new Rerout SDK. The existing
TypeScript (`typescript/src/`) and Dart (`dart/lib/`) SDKs are the canonical
implementations — read them in full before writing anything. This document is
the spec; mismatches with the reference SDKs should follow the reference.

## Identity

- Workspace repo: <https://github.com/ModestNerds-Co/rerout-sdks>
- Maintainer: Codecraft Solutions <hello@codecraftsolutions.co.za>
- License: MIT — copy `/Users/modestnerd/Developer/Projects/rerout-sdks/LICENSE`
  into every package directory.
- Initial version: `0.1.0`

## API base + auth

- Default base URL: `https://api.rerout.co`, trim trailing slashes.
- Auth: every request sends `Authorization: Bearer <api_key>`.
- Content-Type: `application/json` when sending a body.

## Client surface

The client exposes three namespaces. Names are idiomatic per language (snake_case
in Python, camelCase in Kotlin, PascalCase property in C#, etc.) but the
underlying methods and shapes must match.

### links

| Method | HTTP |
|---|---|
| `create(input)` | POST `/v1/links` |
| `list(cursor?, limit?)` | GET `/v1/links` |
| `get(code)` | GET `/v1/links/:code` |
| `update(code, input)` | PATCH `/v1/links/:code` |
| `delete(code)` | DELETE `/v1/links/:code` |
| `stats(code, days=30)` | GET `/v1/links/:code/stats` |

### project

| Method | HTTP |
|---|---|
| `stats(days=30)` | GET `/v1/projects/me/stats` |
| `me()` | GET `/v1/projects/me` |

### qr

| Method | Behaviour |
|---|---|
| `url(code, options?)` | Pure URL builder. No network call. |
| `svg(code, options?)` | GET `/v1/links/:code/qr` with bearer; returns SVG text. |

QR options: `size` (int), `margin` (int), `ecc` (`L`/`M`/`Q`/`H`), `domain` (string),
`refresh` (bool|string — `true` → `1`).

## Request bodies

**CreateLinkInput**

| Field | Required | Type |
|---|---|---|
| `target_url` | yes | string |
| `domain_hostname` | no | string |
| `code` | no | string |
| `expires_at` | no | int (unix seconds) |
| `seo_title` | no | string |
| `seo_description` | no | string |
| `seo_image_url` | no | string |
| `seo_canonical_url` | no | string |
| `seo_noindex` | no | bool |

**UpdateLinkInput** — every field optional. The shape must distinguish *"leave the
field alone"* from *"set the field to null on the server"*. Pick the idiomatic
pattern for the language (sentinel value, `Optional<Optional<T>>`, builder
methods, separate `clear_*` flags — Dart uses `clearExpiresAt: bool` etc.).
Empty payload should error client-side without hitting the API.

Fields: `target_url`, `expires_at`, `is_active`, `seo_title`, `seo_description`,
`seo_image_url`, `seo_canonical_url`, `seo_noindex`.

## Webhook signature verification

Standalone helper (free function, static method, etc.). Signature:

```
verify_rerout_signature(
  raw_body: str,
  signature_header: str,
  secret: str,
  tolerance_seconds: int = 300,  // 0 disables timestamp check
  now: () -> int (optional, for tests)
) -> bool
```

- Header format: `t=<unix>,v1=<hex_hmac_sha256>` — parse case-insensitive.
- HMAC-SHA256 over `"<ts>.<rawbody>"` with `secret` as key.
- Reject (return `false`) when: empty header, empty secret, malformed header,
  missing `t`/`v1`, non-numeric or non-positive `t`, non-hex or odd-length `v1`,
  timestamp outside tolerance, computed HMAC ≠ provided.
- Compare HMACs in constant time.
- Default tolerance 300 seconds.

## Error type

Idiomatic exception or error struct per language with these fields:

- `code` — string. Either the API's stable code (e.g. `bad_target_url`,
  `rate_limited`) or a synthetic client code.
- `status` — int. HTTP status, or `0` for network/timeout failures.
- `message` — string.
- `details`/`path`/`timestamp` — optional, as available from the server.

Synthetic codes when the server didn't send a JSON body:

| Status | Code |
|---|---|
| 401 | `unauthorized` |
| 403 | `forbidden` |
| 404 | `not_found` |
| 429 | `rate_limited` |
| 5xx | `server_error` |
| other 4xx | `client_error` |
| network failure | `network_error` |
| timeout | `timeout` |
| 2xx non-JSON | `unexpected_response` |

Convenience flags: `is_rate_limited` (status == 429), `is_server_error` (500–599).

## Test bar (mandatory)

Use the language's idiomatic test framework + mocked HTTP layer. **Do not hit the
real network in tests.** Required coverage:

1. **Constructor**
   - Required API key (blank/missing raises/throws)
   - `base_url` trailing-slash trimming
   - Namespaces present
2. **Request transport**
   - Bearer auth header on every call
   - `content-type: application/json` only when sending a body
   - Query params for `cursor`/`limit`/`days`
   - JSON body serialization
   - Error parsing — server code + message preserved
   - Synthetic codes for 401/403/404/429/5xx with no body
   - Network failure → `network_error`
   - Timeout → `timeout`
   - `unexpected_response` on 2xx non-JSON
3. **Every links method**
   - At least one success path
   - At least one error path
4. **URL encoding edge cases** — `hello world`, `a+b`, `café`, `go/promo` codes
5. **QR URL builder**
   - Bare URL with no options
   - Every option emitted
   - Custom `base_url` honoured
   - `refresh: true` → `1`; `refresh: "v2"` → `v2`
6. **Webhook signature** (minimum)
   - Valid freshly signed
   - Wrong HMAC (different secret) rejected
   - Body tampered (extra space) rejected
   - Expired (outside tolerance) rejected
   - Exactly at tolerance boundary accepted
   - `tolerance_seconds=0` disables timestamp check
   - Malformed headers rejected (≥7 shapes — empty, `garbage`, `t=,v1=abc`,
     missing `t`, missing `v1`, non-numeric `t`, non-hex `v1`, odd-length `v1`)
   - Casing variations (`T=`/`V1=`) accepted
   - Empty secret rejected
   - Empty header rejected

## File layout (every package)

```
<package-dir>/
  README.md         # install + quickstart + per-namespace usage
  CHANGELOG.md      # v0.1.0 entry
  LICENSE           # copy of root MIT
  .gitignore        # language-appropriate
  <pkg-manifest>    # pyproject.toml, composer.json, go.mod, Cargo.toml, etc.
  <source>/         # implementation
  <tests>/          # the comprehensive test suite
```

`README.md` must include:

- Install snippet for the package manager
- "Hello world" usage example showing one link creation
- Sections for: construction, links, project, qr, webhook signature, error
  handling
- License line pointing at workspace LICENSE
- Repo link

`CHANGELOG.md` entry must list the surface that shipped (matching the TS/Dart
CHANGELOGs in style and detail).

## Idiomatic do/don't

Do:

- Use the language's standard async pattern (async/await, futures, callbacks
  — whatever is native).
- Use the language's standard mocking story (pytest fixtures + respx, Guzzle
  MockHandler, Go httptest.Server, mockito, etc.).
- Mirror the Result<T>/Success/Error pattern from Dart **only** where idiomatic
  (e.g. Rust `Result<T, ReroutError>`). For Python/PHP/Ruby/Kotlin/C# use
  exceptions. For Go use `(T, error)` returns.
- Match the user's existing house style if there are example packages by the
  same author in that language (check the workspace for them; if absent, use
  community conventions).

Don't:

- Pull in a heavy framework when stdlib will do.
- Generate code with a tool — every file should be hand-written, idiomatic, and
  documented.
- Skip tests, README, CHANGELOG, or LICENSE.
- Modify any file outside the package directory you're creating.
- Touch the existing typescript/ or dart/ directories.
- Commit or push — that's the orchestrator's job.

## What "done" looks like

- Package directory exists with all expected files.
- Strict linter / type-checker passes.
- Test runner passes 100%.
- README is publishable as-is to PyPI / Packagist / pub.dev / crates.io /
  RubyGems / Maven Central / NuGet.
- Report back with: file tree, test count, lint status, any blockers.
