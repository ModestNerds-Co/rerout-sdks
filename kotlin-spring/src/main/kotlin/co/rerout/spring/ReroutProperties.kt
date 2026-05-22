/*
 * rerout-spring-boot-starter — Spring Boot auto-configuration for the Rerout
 * branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.spring

import co.rerout.sdk.DEFAULT_BASE_URL
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for the Rerout SDK, bound from the `rerout.*` namespace in
 * `application.properties` / `application.yml`.
 *
 * The first parameter of each class is intentionally non-defaulted: a Kotlin
 * data class whose parameters all carry defaults gets a synthetic no-arg
 * constructor, which makes Spring Boot fall back to setter binding. Keeping one
 * required (nullable) parameter leaves a single constructor, so Spring Boot
 * uses value-object (constructor) binding without further annotation.
 *
 * ## Example
 *
 * ```yaml
 * rerout:
 *   api-key: ${REROUT_API_KEY}
 *   base-url: https://api.rerout.co
 *   webhook:
 *     secret: ${REROUT_WEBHOOK_SECRET}
 *     path: /webhooks/rerout
 *     tolerance-seconds: 300
 * ```
 */
@ConfigurationProperties("rerout")
public data class ReroutProperties(
    /**
     * Project API key (`rrk_…`). Required for the `Rerout` client bean to be
     * created — when absent or blank, no client is auto-configured.
     */
    val apiKey: String?,
    /** API base URL. Defaults to the production endpoint. */
    val baseUrl: String = DEFAULT_BASE_URL,
    /** Inbound webhook handling. */
    val webhook: Webhook = Webhook(secret = null),
) {
    /** Webhook endpoint configuration, bound from `rerout.webhook.*`. */
    public data class Webhook(
        /**
         * Endpoint signing secret (`whsec_…`). When set together with
         * [enabled], the starter registers a controller that verifies and
         * dispatches inbound webhooks.
         */
        val secret: String?,
        /**
         * Whether to register the built-in webhook controller. Defaults to
         * `true`; the controller is only registered when [secret] is also set.
         */
        val enabled: Boolean = true,
        /** Path the webhook controller is mapped to. */
        val path: String = "/webhooks/rerout",
        /**
         * Signature timestamp tolerance window in seconds. `0` disables the
         * staleness check. Defaults to 300 (five minutes).
         */
        val toleranceSeconds: Long = 300,
    )
}
