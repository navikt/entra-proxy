package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheClient
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.current
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.local
import org.springframework.boot.SpringBootVersion
import org.springframework.boot.actuate.info.Info.Builder
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.SpringVersion
import org.springframework.stereotype.Component

@Component
class StartupInfoContributor(private val ctx: ConfigurableApplicationContext, private  val cache: CacheClient, private val buildPropereties: BuildProperties) :
    InfoContributor {

    override fun contribute(builder: Builder) {
        with(ctx) {
            builder.withDetail(
                "extra-info", mapOf(
                    "Cluster" to current.clusterName,
                    "Startup" to startupDate.local(),
                    "Java runtime version" to environment.getProperty("java.version"),
                    "Java runtime version" to environment.getProperty("java.runtime.version"),
                    "Java runtime vendor" to environment.getProperty("java.vm.vendor"),
                    "JDK version" to buildPropereties.get("jdk.version"),
                    "JDK vendor" to buildPropereties.get("jdk.vendor"),
                    "Client ID" to environment.getProperty("azure.app.client.id"),
                    "Name" to environment.getProperty("spring.application.name"),
                    "Caches" to cache.cacheStørrelser(),
                    "Spring Boot version" to SpringBootVersion.getVersion(),
                    "Spring Framework version" to SpringVersion.getVersion()))

        }
    }
}