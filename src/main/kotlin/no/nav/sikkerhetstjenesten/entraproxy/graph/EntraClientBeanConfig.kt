package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesBeanConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesBeanConfig.Companion.headerAddingRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.PingableHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class EntraClientBeanConfig {

    @Bean
    fun entraGraphClient(b: RestClient.Builder, cfg: EntraConfig): EntraGraphClient =
        FellesBeanConfig.createClient(cfg, b.requestInterceptors {
            it.add(headerAddingRequestInterceptor(HEADER_CONSISTENCY_LEVEL))
        })

    @Bean
    fun entraHealthIndicator(a: EntraRestClientAdapter) = PingableHealthIndicator(a)

    companion object {
        private val HEADER_CONSISTENCY_LEVEL = "ConsistencyLevel" to "eventual"
    }
}