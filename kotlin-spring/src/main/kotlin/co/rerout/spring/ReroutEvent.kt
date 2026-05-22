/*
 * rerout-spring-boot-starter — Spring Boot auto-configuration for the Rerout
 * branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.spring

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * A typed, verified inbound Rerout webhook event.
 *
 * Rerout delivers webhooks as a JSON object with at least a `type` discriminator
 * and a `data` payload. This sealed hierarchy maps the known event types to
 * Kotlin classes; anything unrecognised surfaces as [Unknown] so a handler can
 * still observe and log it.
 *
 * Parse a raw, signature-verified body with [ReroutEvent.parse].
 */
public sealed class ReroutEvent {
    /** The raw `type` discriminator string as delivered by Rerout. */
    public abstract val type: String

    /** Unix-seconds timestamp the event was emitted, when present. */
    public abstract val createdAt: Long?

    /** Fired when a short link is created. */
    public data class LinkCreated(
        override val createdAt: Long?,
        /** The short code that was created. */
        val code: String,
        /** The fully-qualified short URL, when present. */
        val shortUrl: String?,
        /** The raw `data` object for fields not lifted onto this class. */
        val data: JsonObject,
    ) : ReroutEvent() {
        override val type: String get() = TYPE

        public companion object {
            /** The `type` discriminator value. */
            public const val TYPE: String = "link.created"
        }
    }

    /** Fired when a short link is updated. */
    public data class LinkUpdated(
        override val createdAt: Long?,
        /** The short code that was updated. */
        val code: String,
        /** The raw `data` object for fields not lifted onto this class. */
        val data: JsonObject,
    ) : ReroutEvent() {
        override val type: String get() = TYPE

        public companion object {
            /** The `type` discriminator value. */
            public const val TYPE: String = "link.updated"
        }
    }

    /** Fired when a short link is deleted. */
    public data class LinkDeleted(
        override val createdAt: Long?,
        /** The short code that was deleted. */
        val code: String,
        /** The raw `data` object for fields not lifted onto this class. */
        val data: JsonObject,
    ) : ReroutEvent() {
        override val type: String get() = TYPE

        public companion object {
            /** The `type` discriminator value. */
            public const val TYPE: String = "link.deleted"
        }
    }

    /** Fired when a short link is clicked or its QR code is scanned. */
    public data class LinkClicked(
        override val createdAt: Long?,
        /** The short code that was clicked. */
        val code: String,
        /** Two-letter country code of the click, when resolved. */
        val country: String?,
        /** Whether the click came from a QR scan. */
        val qr: Boolean,
        /** The raw `data` object for fields not lifted onto this class. */
        val data: JsonObject,
    ) : ReroutEvent() {
        override val type: String get() = TYPE

        public companion object {
            /** The `type` discriminator value. */
            public const val TYPE: String = "link.clicked"
        }
    }

    /**
     * Any event whose `type` the SDK does not (yet) model. The full payload is
     * preserved so handlers can still react to new event types without an SDK
     * upgrade.
     */
    public data class Unknown(
        override val type: String,
        override val createdAt: Long?,
        /** The complete decoded webhook payload. */
        val payload: JsonObject,
    ) : ReroutEvent()

    public companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Parse a raw webhook body into a typed [ReroutEvent].
         *
         * The body should already have passed signature verification.
         *
         * @throws IllegalArgumentException when the body is not a JSON object
         *   or is missing the `type` discriminator.
         */
        @JvmStatic
        public fun parse(rawBody: String): ReroutEvent {
            val root = runCatching { json.parseToJsonElement(rawBody) }
                .getOrNull() as? JsonObject
                ?: throw IllegalArgumentException("Webhook body is not a JSON object.")

            val type = root.string("type")
                ?: throw IllegalArgumentException("Webhook body is missing the `type` field.")
            val createdAt = (root["created_at"] as? JsonPrimitive)?.longOrNull
            val data = (root["data"] as? JsonObject) ?: JsonObject(emptyMap())

            return when (type) {
                LinkCreated.TYPE -> LinkCreated(
                    createdAt = createdAt,
                    code = data.string("code").orEmpty(),
                    shortUrl = data.string("short_url"),
                    data = data,
                )
                LinkUpdated.TYPE -> LinkUpdated(
                    createdAt = createdAt,
                    code = data.string("code").orEmpty(),
                    data = data,
                )
                LinkDeleted.TYPE -> LinkDeleted(
                    createdAt = createdAt,
                    code = data.string("code").orEmpty(),
                    data = data,
                )
                LinkClicked.TYPE -> LinkClicked(
                    createdAt = createdAt,
                    code = data.string("code").orEmpty(),
                    country = data.string("country"),
                    qr = (data["qr"] as? JsonPrimitive)?.contentOrNull?.toBoolean() ?: false,
                    data = data,
                )
                else -> Unknown(type = type, createdAt = createdAt, payload = root)
            }
        }

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.contentOrNull
    }
}
