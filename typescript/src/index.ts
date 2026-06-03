/**
 * @rerout/sdk — Official TypeScript / JavaScript SDK for the Rerout API.
 *
 * Branded link infrastructure on Cloudflare. Create short links, render QR
 * codes, read analytics, and verify webhook signatures.
 *
 * @see https://rerout.co
 * @see https://github.com/ModestNerds-Co/rerout-sdks
 */

export { Rerout, DEFAULT_BASE_URL, Links, Project, Qr, Webhooks } from './client.js'
export type { ReroutClientOptions } from './client.js'
export { ReroutError } from './errors.js'
export {
  verifyReroutSignature,
  DEFAULT_SIGNATURE_TOLERANCE_SECONDS,
} from './webhooks.js'
export type { VerifyOptions } from './webhooks.js'
export { buildQrUrl } from './qr.js'
export type {
  CreateLinkInput,
  CreatedWebhook,
  CreateWebhookInput,
  DailyClicksPoint,
  Link,
  LinkStats,
  ListLinksParams,
  ListLinksResult,
  ListWebhooksResult,
  ProjectStats,
  QrUrlOptions,
  StatsBreakdown,
  Tag,
  UpdateLinkInput,
  Webhook,
  WebhookPayloadFormat,
} from './types.js'
