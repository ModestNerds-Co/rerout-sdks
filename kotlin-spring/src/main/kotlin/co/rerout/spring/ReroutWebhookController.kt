/*
 * rerout-spring-boot-starter — Spring Boot auto-configuration for the Rerout
 * branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.spring

import co.rerout.sdk.verifyReroutSignature
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

/**
 * Header carrying the Rerout webhook HMAC signature.
 */
public const val REROUT_SIGNATURE_HEADER: String = "X-Rerout-Signature"

/**
 * Callback for handling a verified inbound [ReroutEvent].
 *
 * Register one or more beans implementing this interface; the auto-configured
 * [ReroutWebhookController] dispatches every verified event to all of them.
 * Throwing from a handler causes the controller to respond `500` so Rerout
 * retries the delivery.
 *
 * ```kotlin
 * @Component
 * class MyHandler : ReroutWebhookHandler {
 *     override fun onEvent(event: ReroutEvent) {
 *         when (event) {
 *             is ReroutEvent.LinkClicked -> log.info("click on {}", event.code)
 *             else -> {}
 *         }
 *     }
 * }
 * ```
 */
public fun interface ReroutWebhookHandler {
    /** Handle a single verified event. */
    public fun onEvent(event: ReroutEvent)
}

/**
 * REST controller that receives inbound Rerout webhooks, verifies their
 * signature, parses the payload into a [ReroutEvent], and dispatches it to
 * every registered [ReroutWebhookHandler].
 *
 * Registered automatically by [ReroutAutoConfiguration] when
 * `rerout.webhook.secret` is set and `rerout.webhook.enabled` is `true`. The
 * mapping path is configurable via `rerout.webhook.path`.
 *
 * Responses:
 * - `200` — verified and dispatched.
 * - `400` — missing/invalid signature, or an unparseable body.
 * - `500` — a handler threw; Rerout will retry.
 */
@RestController
public class ReroutWebhookController(
    private val properties: ReroutProperties,
    private val handlers: List<ReroutWebhookHandler>,
) {
    private val log = LoggerFactory.getLogger(ReroutWebhookController::class.java)

    /**
     * Receive a webhook delivery. Mapped to `rerout.webhook.path` via the
     * `reroutWebhookHandlerMapping` bean in [ReroutAutoConfiguration]; the
     * `@PostMapping` here is a fallback for the default path.
     */
    @PostMapping("\${rerout.webhook.path:/webhooks/rerout}")
    public fun receive(
        @RequestBody rawBody: String,
        @RequestHeader(name = REROUT_SIGNATURE_HEADER, required = false) signature: String?,
    ): ResponseEntity<String> {
        val secret = properties.webhook.secret
        if (secret.isNullOrBlank()) {
            log.warn("Rerout webhook received but rerout.webhook.secret is not configured.")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("webhook secret not configured")
        }

        val verified = verifyReroutSignature(
            rawBody = rawBody,
            signatureHeader = signature,
            secret = secret,
            toleranceSeconds = properties.webhook.toleranceSeconds,
        )
        if (!verified) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature")
        }

        val event = try {
            ReroutEvent.parse(rawBody)
        } catch (e: IllegalArgumentException) {
            log.warn("Rerout webhook body could not be parsed: {}", e.message)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("unparseable body")
        }

        for (handler in handlers) {
            handler.onEvent(event)
        }
        return ResponseEntity.ok("ok")
    }
}
