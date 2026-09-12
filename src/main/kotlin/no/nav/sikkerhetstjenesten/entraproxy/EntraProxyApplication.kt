package no.nav.sikkerhetstjenesten.entraproxy

import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.profiler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableResilientMethods
@EnableAspectJAutoProxy
@EnableScheduling
@EnableMethodSecurity
class EntraProxyApplication

fun main(args: Array<String>) {
    runApplication<EntraProxyApplication>(*args) {
        setAdditionalProfiles(*profiler)
    }
}

