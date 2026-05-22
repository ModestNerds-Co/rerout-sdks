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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Link operations namespace — create, list, get, update, delete, stats.
 * Reached via [Rerout.links].
 */
public class Links internal constructor(
    private val transport: HttpTransport,
) {
    /** Creates a new short link. */
    public suspend fun create(input: CreateLinkInput): Link {
        val path = "/v1/links"
        val body = transport.jsonFormat.encodeToString(CreateLinkInput.serializer(), input)
        val text = transport.execute(
            HttpRequest(method = HttpMethod.POST, path = path, body = body),
        )
        return transport.decode<Link>(text, path)
    }

    /** Paginated list of links in the project. */
    @JvmOverloads
    public suspend fun list(cursor: Long? = null, limit: Int? = null): ListLinksResult {
        val path = "/v1/links"
        val query = LinkedHashMap<String, String>()
        if (cursor != null) query["cursor"] = cursor.toString()
        if (limit != null) query["limit"] = limit.toString()
        val text = transport.execute(
            HttpRequest(method = HttpMethod.GET, path = path, query = query),
        )
        return transport.decode<ListLinksResult>(text, path)
    }

    /** Gets a single link by code. */
    public suspend fun get(code: String): Link {
        val path = "/v1/links/${code.encodePathSegment()}"
        val text = transport.execute(HttpRequest(method = HttpMethod.GET, path = path))
        return transport.decode<Link>(text, path)
    }

    /**
     * Patches a link. Only fields set on [input] are sent — an empty patch is
     * rejected client-side without hitting the API.
     */
    public suspend fun update(code: String, input: UpdateLinkInput): Link {
        val path = "/v1/links/${code.encodePathSegment()}"
        if (input.isEmpty) {
            throw ReroutException(
                code = "bad_request",
                message = "UpdateLinkInput has no fields to send.",
                status = 0,
                path = path,
            )
        }
        val body = encodePatch(input.toJsonMap())
        val text = transport.execute(
            HttpRequest(method = HttpMethod.PATCH, path = path, body = body),
        )
        return transport.decode<Link>(text, path)
    }

    /**
     * Soft-deletes a link. The short URL stops redirecting and is gone from
     * lists.
     */
    public suspend fun delete(code: String): DeleteResult {
        val path = "/v1/links/${code.encodePathSegment()}"
        val text = transport.execute(HttpRequest(method = HttpMethod.DELETE, path = path))
        if (text.isEmpty()) return DeleteResult(deleted = true)
        return transport.decode<DeleteResult>(text, path)
    }

    /** Per-link click stats. Defaults to 30 days. */
    @JvmOverloads
    public suspend fun stats(code: String, days: Int = 30): LinkStats {
        val path = "/v1/links/${code.encodePathSegment()}/stats"
        val text = transport.execute(
            HttpRequest(
                method = HttpMethod.GET,
                path = path,
                query = mapOf("days" to days.toString()),
            ),
        )
        return transport.decode<LinkStats>(text, path)
    }

    private fun encodePatch(map: Map<String, Any?>): String {
        val obj: JsonObject = buildJsonObject {
            for ((key, value) in map) {
                when (value) {
                    null -> put(key, JsonNull)
                    is String -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, JsonPrimitive(value))
                    is Long -> put(key, JsonPrimitive(value))
                    is Int -> put(key, JsonPrimitive(value))
                    else -> put(key, JsonPrimitive(value.toString()))
                }
            }
        }
        return transport.jsonFormat.encodeToString(JsonObject.serializer(), obj)
    }
}
