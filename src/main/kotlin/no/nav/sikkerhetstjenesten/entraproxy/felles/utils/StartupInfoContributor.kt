package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheClient
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.current
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.local
import org.springframework.boot.SpringBootVersion
import org.springframework.boot.actuate.info.Info.Builder
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.SpringVersion
import org.springframework.stereotype.Component

@Component
class StartupInfoContributor(private val ctx: ConfigurableApplicationContext, private  val cache: CacheClient) :
    InfoContributor {

    override fun contribute(builder: Builder) {
        with(ctx) {
            builder.withDetail(
                "extra-info", mapOf(
                    "Cluster" to current.clusterName,
                    "Startup" to startupDate.local(),
                    "Java runtime version" to environment.getProperty("java.runtime.version"),
                    "Java vendor" to environment.getProperty("java.vm.vendor"),
                    "Client ID" to environment.getProperty("azure.app.client.id"),
                    "Name" to environment.getProperty("spring.application.name"),
                    "Caches" to cache.cacheStørrelser(),
                    "Spring Boot version" to SpringBootVersion.getVersion(),
                    "Spring Framework version" to SpringVersion.getVersion()))

        }
    }
}