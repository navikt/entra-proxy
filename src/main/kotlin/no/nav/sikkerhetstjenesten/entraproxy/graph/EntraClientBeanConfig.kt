package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesBeanConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesBeanConfig.Companion.headerAddingRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.PingableHealthIndicator
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.service.registry.ImportHttpServices

@Configuration
@ImportHttpServices(types = [EntraGraphClient::class], group = GRAPH)
class EntraClientBeanConfig {

    @Bean
    fun entraProxyHealthIndicator(cfg: EntraConfig, client: EntraGraphClient) =
        PingableHealthIndicator(cfg, client::ping)
}