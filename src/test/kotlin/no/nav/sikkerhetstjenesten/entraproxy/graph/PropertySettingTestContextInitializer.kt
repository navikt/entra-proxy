package no.nav.sikkerhetstjenesten.entraproxy.graph

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils

class PropertySettingTestContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(ctx: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addPropertiesFilesToEnvironment(ctx, "classpath:test.properties")
        val values = serviceClientBaseUrls()
            .map { (client, url) -> "${OAuth2ClientTestConfig.SERVICE_CLIENT_PREFIX}.$client.base-url=$url" }
            .toTypedArray()
        TestPropertyValues.of(*values).applyTo(ctx)
    }

    companion object {
        private fun serviceClientBaseUrls() = mapOf(
            "norg" to "http://pdlpip.pdl",
            "graph" to "http://pdlgraph.pdl",
        )
    }
}