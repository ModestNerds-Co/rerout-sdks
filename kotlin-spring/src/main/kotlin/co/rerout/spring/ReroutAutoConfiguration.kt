/*
 * rerout-spring-boot-starter — Spring Boot auto-configuration for the Rerout
 * branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.spring

import co.rerout.sdk.Rerout
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * Condition that matches when `rerout.webhook.secret` is present and non-blank.
 *
 * `@ConditionalOnProperty` treats an empty string as "present", so a dedicated
 * condition is used to keep the webhook controller from registering with a
 * blank signing secret.
 */
internal class WebhookSecretPresentCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        !context.environment.getProperty("rerout.webhook.secret").isNullOrBlank()
}

/**
 * Spring Boot auto-configuration for the Rerout SDK.
 *
 * Registers:
 * - a [Rerout] client bean, built from `rerout.api-key` / `rerout.base-url` —
 *   only when `rerout.api-key` is set and no [Rerout] bean already exists;
 * - a [ReroutWebhookController] — only in a servlet web application, when
 *   `rerout.webhook.secret` is set and `rerout.webhook.enabled` is `true`
 *   (the default).
 *
 * Both beans back off if the application defines its own, so an app can fully
 * override the client or the webhook controller.
 */
@AutoConfiguration
@EnableConfigurationProperties(ReroutProperties::class)
public class ReroutAutoConfiguration {

    /**
     * The Rerout API client. Created only when `rerout.api-key` carries a
     * non-empty value; back off if the application already supplies one.
     */
    @Bean
    @ConditionalOnMissingBean(Rerout::class)
    @ConditionalOnProperty(prefix = "rerout", name = ["api-key"])
    public fun reroutClient(properties: ReroutProperties): Rerout {
        val apiKey = properties.apiKey
        require(!apiKey.isNullOrBlank()) {
            "rerout.api-key must not be blank when a Rerout client bean is requested."
        }
        return Rerout(apiKey = apiKey, baseUrl = properties.baseUrl)
    }

    /**
     * The inbound webhook controller. Registered only in a servlet web
     * application, when a webhook secret is configured and webhook handling
     * has not been disabled.
     */
    @Bean
    @ConditionalOnMissingBean(ReroutWebhookController::class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "rerout.webhook", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    @Conditional(WebhookSecretPresentCondition::class)
    public fun reroutWebhookController(
        properties: ReroutProperties,
        handlers: List<ReroutWebhookHandler>,
    ): ReroutWebhookController = ReroutWebhookController(properties, handlers)
}
