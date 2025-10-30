package no.nav.entraproxy

import no.nav.sikkerhetstjenesten.entraproxy.EntraProxyApplication
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.profiler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.retry.annotation.EnableRetry



@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableRetry
class TestApp
fun main(args: Array<String>) {
    runApplication<EntraProxyApplication>(*args) {
        setAdditionalProfiles(*profiler)
    }
}

