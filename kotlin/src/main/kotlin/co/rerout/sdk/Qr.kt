/*
 * rerout-kotlin — Official Kotlin/JVM SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk

import co.rerout.sdk.internal.HttpMethod
import co.rerout.sdk.internal.HttpRequest
import co.rerout.sdk.internal.HttpTransport
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * QR helpers — URL builders and authenticated SVG fetch. Reached via
 * [Rerout.qr].
 */
public class Qr internal constructor(
    private val transport: HttpTransport,
    private val resolvedBaseUrl: String,
) {
    /**
     * Builds the URL the Rerout API serves the QR SVG from. Pure — does not
     * call the API.
     *
     * Authentication is the caller's responsibility — the endpoint is
     * API-key authenticated, so an `<img>` tag needs a server-side proxy to
     * attach the bearer token.
     *
     * @sample
     * ```kotlin
     * val url = rerout.qr.url("q4", QrOptions(size = 12, ecc = "H"))
     * ```
     */
    @JvmOverloads
    public fun url(code: String, options: QrOptions? = null): String {
        val builder = "$resolvedBaseUrl/v1/links/${code.encodePathSegment()}/qr"
            .toHttpUrl()
            .newBuilder()
        val params = options?.toQueryParameters() ?: emptyMap()
        for ((key, value) in params) {
            builder.addQueryParameter(key, value)
        }
        return builder.build().toString()
    }

    /**
     * Fetches the QR SVG body. Hits the same endpoint as [url] but attaches
     * the bearer token and returns the rendered text.
     */
    @JvmOverloads
    public suspend fun svg(code: String, options: QrOptions? = null): String {
        val path = "/v1/links/${code.encodePathSegment()}/qr"
        val body = transport.execute(
            HttpRequest(
                method = HttpMethod.GET,
                path = path,
                query = options?.toQueryParameters() ?: emptyMap(),
            ),
        )
        if (body.isEmpty()) {
            throw ReroutException(
                code = "unexpected_response",
                message = "Rerout returned an empty QR body.",
                status = 200,
                path = path,
            )
        }
        return body
    }
}
