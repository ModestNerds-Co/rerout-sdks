/*
 * rerout-spring-boot-starter — auto-configuration tests.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.spring

import co.rerout.sdk.Rerout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class AutoConfigurationTest {

    private val baseRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ReroutAutoConfiguration::class.java))

    private val webRunner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ReroutAutoConfiguration::class.java))

    // ─── client bean ────────────────────────────────────────────────────────

    @Test
    fun `a Rerout client bean is created when an api key is set`() {
        baseRunner
            .withPropertyValues("rerout.api-key=rrk_test")
            .run { context ->
                assertTrue(context.containsBean("reroutClient"))
                val client = context.getBean(Rerout::class.java)
                assertEquals("https://api.rerout.co", client.baseUrl)
            }
    }

    @Test
    fun `no Rerout client bean is created when the api key is absent`() {
        baseRunner.run { context ->
            assertFalse(context.containsBean("reroutClient"))
        }
    }

    @Test
    fun `the client base url is taken from properties`() {
        baseRunner
            .withPropertyValues(
                "rerout.api-key=rrk_test",
                "rerout.base-url=https://api.staging.rerout.co",
            )
            .run { context ->
                assertEquals(
                    "https://api.staging.rerout.co",
                    context.getBean(Rerout::class.java).baseUrl,
                )
            }
    }

    @Test
    fun `an application-supplied Rerout bean wins over the auto-configured one`() {
        baseRunner
            .withPropertyValues("rerout.api-key=rrk_test")
            .withUserConfiguration(CustomClientConfig::class.java)
            .run { context ->
                assertSame(
                    CustomClientConfig.CUSTOM,
                    context.getBean(Rerout::class.java),
                )
            }
    }

    // ─── properties binding ─────────────────────────────────────────────────

    @Test
    fun `properties bind from the rerout namespace`() {
        baseRunner
            .withPropertyValues(
                "rerout.api-key=rrk_abc",
                "rerout.base-url=https://example.test",
                "rerout.webhook.secret=whsec_xyz",
                "rerout.webhook.path=/hooks/in",
                "rerout.webhook.tolerance-seconds=120",
                "rerout.webhook.enabled=false",
            )
            .run { context ->
                val props = context.getBean(ReroutProperties::class.java)
                assertEquals("rrk_abc", props.apiKey)
                assertEquals("https://example.test", props.baseUrl)
                assertEquals("whsec_xyz", props.webhook.secret)
                assertEquals("/hooks/in", props.webhook.path)
                assertEquals(120L, props.webhook.toleranceSeconds)
                assertFalse(props.webhook.enabled)
            }
    }

    @Test
    fun `webhook properties have sane defaults`() {
        baseRunner.run { context ->
            val props = context.getBean(ReroutProperties::class.java)
            assertEquals("/webhooks/rerout", props.webhook.path)
            assertEquals(300L, props.webhook.toleranceSeconds)
            assertTrue(props.webhook.enabled)
        }
    }

    // ─── webhook controller registration ────────────────────────────────────

    @Test
    fun `the webhook controller is registered in a web app with a secret`() {
        webRunner
            .withPropertyValues("rerout.webhook.secret=whsec_test")
            .run { context ->
                assertTrue(context.containsBean("reroutWebhookController"))
            }
    }

    @Test
    fun `no webhook controller is registered without a secret`() {
        webRunner.run { context ->
            assertFalse(context.containsBean("reroutWebhookController"))
        }
    }

    @Test
    fun `no webhook controller is registered for a blank secret`() {
        webRunner
            .withPropertyValues("rerout.webhook.secret=")
            .run { context ->
                assertFalse(context.containsBean("reroutWebhookController"))
            }
    }

    @Test
    fun `no webhook controller is registered when webhooks are disabled`() {
        webRunner
            .withPropertyValues(
                "rerout.webhook.secret=whsec_test",
                "rerout.webhook.enabled=false",
            )
            .run { context ->
                assertFalse(context.containsBean("reroutWebhookController"))
            }
    }

    @Test
    fun `no webhook controller is registered outside a web application`() {
        baseRunner
            .withPropertyValues("rerout.webhook.secret=whsec_test")
            .run { context ->
                assertFalse(context.containsBean("reroutWebhookController"))
            }
    }

    @Test
    fun `the context starts cleanly with no rerout configuration at all`() {
        baseRunner.run { context ->
            assertTrue(context.startupFailure == null)
            // The properties bean is always present; the optional client and
            // webhook beans back off without any `rerout.*` configuration.
            assertTrue(context.containsBean("rerout-co.rerout.spring.ReroutProperties"))
            assertFalse(context.containsBean("reroutClient"))
        }
    }

    @Configuration
    open class CustomClientConfig {
        @Bean
        open fun reroutClient(): Rerout = CUSTOM

        companion object {
            val CUSTOM: Rerout = Rerout(apiKey = "rrk_custom")
        }
    }
}
