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
 * Project-level operations namespace — aggregate stats and project info.
 * Reached via [Rerout.project].
 */
public class Project internal constructor(
    private val transport: HttpTransport,
) {
    /** Aggregate stats across every link in the project. Defaults to 30 days. */
    @JvmOverloads
    public suspend fun stats(days: Int = 30): ProjectStats {
        val path = "/v1/projects/me/stats"
        val text = transport.execute(
            HttpRequest(
                method = HttpMethod.GET,
                path = path,
                query = mapOf("days" to days.toString()),
            ),
        )
        return transport.decode<ProjectStats>(text, path)
    }

    /** Returns info about the project that owns the current API key. */
    public suspend fun me(): ProjectInfo {
        val path = "/v1/projects/me"
        val text = transport.execute(HttpRequest(method = HttpMethod.GET, path = path))
        return transport.decode<ProjectInfo>(text, path)
    }
}
