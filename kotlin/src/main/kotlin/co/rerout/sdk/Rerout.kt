/*
 * rerout-kotlin — Official Kotlin/JVM SDK for the Rerout branded-link API.
 *
 * Branded link infrastructure on the edge. Create short links, render QR
 * codes, read analytics, and verify webhook signatures.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 *
 * @see https://rerout.co
 * @see https://github.com/ModestNerds-Co/rerout-sdks
 */

package co.rerout.sdk

import co.rerout.sdk.internal.HttpTransport
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.time.Duration

/** Default production API URL. Override via `baseUrl` for staging / self-hosted. */
public const val DEFAULT_BASE_URL: String = "https://api.rerout.co"

/** Default per-request timeout. */
private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(30)

/** SDK version, surfaced in the `User-Agent` header. */
internal const val SDK_VERSION: String = "0.4.0"

/**
 * Official client for the Rerout API.
 *
 * The client exposes five namespaces: [links], [project], [qr], [webhooks],
 * and [conversions]. All network calls are `suspend` functions and throw
 * [ReroutException] on any failure.
 *
 * ## Usage
 *
 * ```kotlin
 * import co.rerout.sdk.Rerout
 * import co.rerout.sdk.CreateLinkInput
 *
 * val rerout = Rerout(apiKey = System.getenv("REROUT_API_KEY"))
 *
 * val link = rerout.links.create(
 *     CreateLinkInput(targetUrl = "https://example.com/q4-sale", code = "q4"),
 * )
 * println(link.shortUrl)
 * ```
 */
public class Rerout private constructor(
    transport: HttpTransport,
    /** The resolved API base URL — trailing slashes trimmed. */
    public val baseUrl: String,
) {
    /** Link operations: create, list, get, update, delete, stats. */
    public val links: Links = Links(transport)

    /** Project-level operations: aggregate stats, current project. */
    public val project: Project = Project(transport)

    /** QR helpers — URL builders and authenticated SVG fetch. */
    public val qr: Qr = Qr(transport, baseUrl)

    /** Webhook endpoint management: create, list, delete. */
    public val webhooks: Webhooks = Webhooks(transport)

    /** Conversion tracking: record a conversion against a prior click. */
    public val conversions: Conversions = Conversions(transport)

    public companion object {
        /**
         * Creates a Rerout client.
         *
         * @param apiKey Project API key from the dashboard (`rrk_…`). Required;
         *   a blank value throws [ReroutException] with code `missing_api_key`.
         * @param baseUrl Override the API base URL. Defaults to
         *   [DEFAULT_BASE_URL]. Trailing slashes are trimmed.
         * @param httpClient Inject a pre-configured [OkHttpClient] — useful for
         *   tests, custom interceptors, proxies, or shared connection pools.
         *   When omitted, a client with a 30s timeout is created.
         * @param json Inject a custom [Json] instance. When omitted, a lenient
         *   instance that ignores unknown keys is used.
         */
        @JvmStatic
        @JvmOverloads
        public operator fun invoke(
            apiKey: String,
            baseUrl: String = DEFAULT_BASE_URL,
            httpClient: OkHttpClient? = null,
            json: Json? = null,
        ): Rerout {
            if (apiKey.isBlank()) {
                throw ReroutException(
                    code = "missing_api_key",
                    message = "A project API key is required to construct Rerout.",
                    status = 0,
                )
            }
            val resolvedBaseUrl = baseUrl.trimEnd('/')
            if (resolvedBaseUrl.isEmpty()) {
                throw ReroutException(
                    code = "bad_request",
                    message = "baseUrl must not be empty.",
                    status = 0,
                )
            }
            val resolvedClient = httpClient ?: OkHttpClient.Builder()
                .callTimeout(DEFAULT_TIMEOUT)
                .connectTimeout(DEFAULT_TIMEOUT)
                .readTimeout(DEFAULT_TIMEOUT)
                .writeTimeout(DEFAULT_TIMEOUT)
                .build()
            val resolvedJson = json ?: Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = false
            }
            val transport = HttpTransport(
                apiKey = apiKey,
                baseUrl = resolvedBaseUrl,
                client = resolvedClient,
                json = resolvedJson,
                userAgent = "rerout-kotlin/$SDK_VERSION",
            )
            return Rerout(transport, resolvedBaseUrl)
        }
    }
}

/**
 * URL-encode a value for use as a single path segment.
 *
 * `URLEncoder` targets `application/x-www-form-urlencoded`, which encodes
 * spaces as `+`. Path segments need `%20`, so the `+` is fixed up afterwards.
 */
internal fun String.encodePathSegment(): String =
    URLEncoder.encode(this, Charsets.UTF_8).replace("+", "%20")
