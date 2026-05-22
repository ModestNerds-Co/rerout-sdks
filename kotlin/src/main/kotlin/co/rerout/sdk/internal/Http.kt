/*
 * rerout-kotlin — Official Kotlin/JVM SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.internal

import co.rerout.sdk.ReroutException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.resumeWithException

/** Supported HTTP verbs for the Rerout API. */
internal enum class HttpMethod { GET, POST, PATCH, DELETE }

/** A fully-described request handed to [HttpTransport.execute]. */
internal data class HttpRequest(
    val method: HttpMethod,
    val path: String,
    val query: Map<String, String> = emptyMap(),
    /** Pre-serialized JSON body, or `null` for no body. */
    val body: String? = null,
)

/**
 * Raw transport over OkHttp. Handles auth, query params, error parsing, and
 * the JSON / SVG content split. Stays internal so the public surface only
 * exposes idiomatic suspending namespace methods.
 */
internal class HttpTransport(
    private val apiKey: String,
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val json: Json,
    private val userAgent: String,
) {
    // A bare `application/json` media type — no charset suffix. Sending the
    // body as a ByteArray keeps OkHttp from appending `; charset=utf-8`.
    private val jsonMediaType = "application/json".toMediaType()

    /** Execute [request] and return the response body text on success. */
    suspend fun execute(request: HttpRequest): String {
        val urlBuilder = (baseUrl + request.path).toHttpUrl().newBuilder()
        for ((key, value) in request.query) {
            urlBuilder.addQueryParameter(key, value)
        }

        val builder = Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json, image/svg+xml, text/plain")
            .header("User-Agent", userAgent)

        when (request.method) {
            HttpMethod.GET -> builder.get()
            HttpMethod.DELETE ->
                if (request.body != null) {
                    builder.delete(jsonBody(request.body))
                } else {
                    builder.delete()
                }
            HttpMethod.POST -> builder.post(jsonBody(request.body ?: ""))
            HttpMethod.PATCH -> builder.patch(jsonBody(request.body ?: ""))
        }

        try {
            client.newCall(builder.build()).await().use { resp ->
                val text = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    throw parseError(resp.code, text, request.path)
                }
                return text
            }
        } catch (e: InterruptedIOException) {
            throw ReroutException(
                code = "timeout",
                message = "Request to Rerout timed out: ${e.message ?: "no detail"}",
                status = 0,
                path = request.path,
                details = e,
                cause = e,
            )
        } catch (e: IOException) {
            throw ReroutException(
                code = "network_error",
                message = "Request to Rerout failed before the server replied: ${e.message ?: "no detail"}",
                status = 0,
                path = request.path,
                details = e,
                cause = e,
            )
        }
    }

    private fun jsonBody(body: String) =
        body.toByteArray(Charsets.UTF_8).toRequestBody(jsonMediaType)

    /** Decode a JSON success body into [T]. */
    inline fun <reified T> decode(text: String, path: String): T {
        if (text.isEmpty()) {
            throw ReroutException(
                code = "unexpected_response",
                message = "Rerout returned an empty success body.",
                status = 200,
                path = path,
            )
        }
        return try {
            json.decodeFromString<T>(text)
        } catch (e: Exception) {
            throw ReroutException(
                code = "unexpected_response",
                message = "Rerout returned a body that could not be parsed as the expected type.",
                status = 200,
                path = path,
                details = text,
                cause = e,
            )
        }
    }

    /** The configured [Json] instance — exposed for [decode]. */
    val jsonFormat: Json get() = json

    private fun parseError(status: Int, body: String, path: String): ReroutException {
        if (body.isEmpty()) {
            return ReroutException(
                code = synthCodeForStatus(status),
                message = "Rerout returned HTTP $status with no body.",
                status = status,
                path = path,
            )
        }
        return try {
            val obj = json.parseToJsonElement(body) as? JsonObject
            val code = (obj?.get("code") as? JsonPrimitive)?.contentOrNull
            val message = (obj?.get("message") as? JsonPrimitive)?.contentOrNull
            val timestamp = (obj?.get("timestamp") as? JsonPrimitive)?.contentOrNull
            ReroutException(
                code = code ?: synthCodeForStatus(status),
                message = message ?: "Rerout returned HTTP $status.",
                status = status,
                path = path,
                timestamp = timestamp,
                details = body,
            )
        } catch (_: Exception) {
            ReroutException(
                code = synthCodeForStatus(status),
                message = "Rerout returned HTTP $status (non-JSON body).",
                status = status,
                path = path,
                details = body,
            )
        }
    }

    internal companion object {
        /** Map a bare HTTP status (no JSON error body) to a synthetic code. */
        fun synthCodeForStatus(status: Int): String = when {
            status == 401 -> "unauthorized"
            status == 403 -> "forbidden"
            status == 404 -> "not_found"
            status == 429 -> "rate_limited"
            status in 500..599 -> "server_error"
            else -> "client_error"
        }
    }
}

/** Suspend until [Call] completes, bridging OkHttp's callback API to coroutines. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            cont.resume(response) { _, value, _ -> value.closeQuietly() }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (cont.isCancelled) return
            cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation {
        runCatching { cancel() }
    }
}

private fun Response.closeQuietly() {
    runCatching { close() }
}
