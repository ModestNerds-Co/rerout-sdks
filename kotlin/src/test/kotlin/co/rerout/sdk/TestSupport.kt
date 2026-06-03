/*
 * rerout-kotlin — test support.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

/** A sample link JSON body reused across tests. */
internal const val SAMPLE_LINK_JSON: String = """
{
  "code": "q4",
  "short_url": "https://go.brand.com/q4",
  "domain_hostname": "go.brand.com",
  "target_url": "https://example.com/q4-sale",
  "project_id": "prj_123",
  "expires_at": null,
  "is_active": true,
  "seo_title": "Q4 Sale",
  "seo_description": null,
  "seo_image_url": null,
  "seo_canonical_url": null,
  "seo_noindex": false,
  "seo_updated_at": null,
  "created_at": 1716000000,
  "updated_at": 1716000000,
  "tags": [
    { "id": "tag_1", "name": "Marketing", "color": "#3b82f6" }
  ]
}
"""

/** A sample link JSON body with no `tags` field, to exercise the lenient default. */
internal const val SAMPLE_LINK_JSON_NO_TAGS: String = """
{
  "code": "q4",
  "short_url": "https://go.brand.com/q4",
  "domain_hostname": "go.brand.com",
  "target_url": "https://example.com/q4-sale",
  "project_id": "prj_123",
  "expires_at": null,
  "is_active": true,
  "seo_title": "Q4 Sale",
  "seo_description": null,
  "seo_image_url": null,
  "seo_canonical_url": null,
  "seo_noindex": false,
  "seo_updated_at": null,
  "created_at": 1716000000,
  "updated_at": 1716000000
}
"""

/** A sample webhook endpoint JSON body reused across tests. */
internal const val SAMPLE_WEBHOOK_JSON: String = """
{
  "id": "wh_abc123",
  "project_id": "prj_test",
  "name": "Order events",
  "url": "https://example.com/hooks/rerout",
  "events": ["link.created", "link.clicked"],
  "is_active": true,
  "payload_format": "json",
  "created_at": 1700000000,
  "updated_at": 1700000000,
  "last_delivery_at": null,
  "last_success_at": null,
  "last_failure_at": null
}
"""

/** A `MockResponse` carrying a JSON body and a 200 status. */
internal fun jsonResponse(body: String, status: Int = 200): MockResponse =
    MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(body.trimIndent())

/** A `MockResponse` carrying plain text. */
internal fun textResponse(body: String, status: Int = 200, contentType: String = "image/svg+xml"): MockResponse =
    MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", contentType)
        .setBody(body)

/** Build a Rerout client pointed at [server]. */
internal fun MockWebServer.client(apiKey: String = "rrk_test"): Rerout =
    Rerout(apiKey = apiKey, baseUrl = url("/").toString().trimEnd('/'))
