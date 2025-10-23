package no.nav.sikkerhetstjenesten.entraproxy

import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheAdapter
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.profiler
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.local
import org.springframework.boot.SpringBootVersion
import org.springframework.boot.actuate.info.Info.Builder
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.SpringVersion
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableOAuth2Client(cacheEnabled = true)
@EnableCaching
@EnableRetry
@EnableScheduling
@EnableJwtTokenValidation(ignore = ["org.springdoc", "org.springframework"])
class EntraProxyApplication

fun main(args: Array<String>) {
    runApplication<EntraProxyApplication>(*args) {
        setAdditionalProfiles(*profiler)
    }
}

@Component
class StartupInfoContributor(private val ctx: ConfigurableApplicationContext, private  val cache: CacheAdapter) :
    InfoContributor {

    override fun contribute(builder: Builder) {
        with(ctx) {
            builder.withDetail(
                "extra-info", mapOf(
                    "Cluster" to ClusterUtils.current.clusterName,
                    "Startup" to startupDate.local(),
                    "Java runtime version" to environment.getProperty("java.runtime.version"),
                    "Java vendor" to environment.getProperty("java.vm.vendor"),
                    "Client ID" to environment.getProperty("azure.app.client.id"),
                    "Name" to environment.getProperty("spring.application.name"),
                    cache.name to cache.cacheSizes(),
                    "Spring Boot version" to SpringBootVersion.getVersion(),
                    "Spring Framework version" to SpringVersion.getVersion()))

        }
    }
}
