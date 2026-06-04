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
 * Conversion tracking namespace — record conversions against prior clicks.
 * Reached via [Rerout.conversions].
 */
public class Conversions internal constructor(
    private val transport: HttpTransport,
) {
    /**
     * Records a conversion attributed to a prior click via its `clickId`
     * (`rrid`). Idempotent per `(clickId, eventName)`: a repeat call returns
     * [RecordedConversion.duplicate] set to `true` without double-counting.
     */
    public suspend fun record(input: RecordConversionInput): RecordedConversion {
        val path = "/v1/conversions"
        val body = transport.jsonFormat.encodeToString(RecordConversionInput.serializer(), input)
        val text = transport.execute(
            HttpRequest(method = HttpMethod.POST, path = path, body = body),
        )
        return transport.decode<RecordedConversion>(text, path)
    }
}
