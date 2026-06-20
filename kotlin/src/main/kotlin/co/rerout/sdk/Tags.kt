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

/**
 * Tag management namespace — list, create, update, delete. Reached via
 * [Rerout.tags].
 *
 * Tags are API-key-authenticated and resolved from the project that owns the
 * key — all calls hit `/v1/projects/me/tags` with no project id in the path.
 */
public class Tags internal constructor(
    private val transport: HttpTransport,
) {
    /** Lists the project's tags, each with its live link count. */
    public suspend fun list(): ListTagsResult {
        val path = "/v1/projects/me/tags"
        val text = transport.execute(HttpRequest(method = HttpMethod.GET, path = path))
        return transport.decode<ListTagsResult>(text, path)
    }

    /**
     * Creates a tag. [CreateTagInput.color] is optional — when omitted the
     * server validates and defaults it (`teal`). Returns the new [Tag].
     */
    public suspend fun create(input: CreateTagInput): Tag {
        val path = "/v1/projects/me/tags"
        val body = transport.jsonFormat.encodeToString(CreateTagInput.serializer(), input)
        val text = transport.execute(
            HttpRequest(method = HttpMethod.POST, path = path, body = body),
        )
        return transport.decode<Tag>(text, path)
    }

    /**
     * Patches a tag's name and/or color. Only fields set on [input] are sent;
     * an empty patch is rejected client-side without hitting the API, mirroring
     * [Links.update].
     */
    public suspend fun update(tagId: String, input: UpdateTagInput): Tag {
        val path = "/v1/projects/me/tags/${tagId.encodePathSegment()}"
        if (input.isEmpty) {
            throw ReroutException(
                code = "bad_request",
                message = "UpdateTagInput has no fields to send.",
                status = 0,
                path = path,
            )
        }
        val body = transport.jsonFormat.encodeToString(UpdateTagInput.serializer(), input)
        val text = transport.execute(
            HttpRequest(method = HttpMethod.PATCH, path = path, body = body),
        )
        return transport.decode<Tag>(text, path)
    }

    /**
     * Deletes a tag and drops its assignments from every link it was attached
     * to.
     */
    public suspend fun delete(tagId: String): DeleteResult {
        val path = "/v1/projects/me/tags/${tagId.encodePathSegment()}"
        val text = transport.execute(HttpRequest(method = HttpMethod.DELETE, path = path))
        if (text.isEmpty()) return DeleteResult(deleted = true)
        return transport.decode<DeleteResult>(text, path)
    }
}
