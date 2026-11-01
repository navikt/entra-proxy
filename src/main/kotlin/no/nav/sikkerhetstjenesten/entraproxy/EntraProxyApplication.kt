package no.nav.sikkerhetstjenesten.entraproxy

import io.valkey.springframework.boot.actuate.autoconfigure.metrics.valkey.LettuceObservationAutoConfiguration
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterUtils.Companion.profiler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication(exclude = [DataRedisRepositoriesAutoConfiguration::class, LettuceObservationAutoConfiguration::class])
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
