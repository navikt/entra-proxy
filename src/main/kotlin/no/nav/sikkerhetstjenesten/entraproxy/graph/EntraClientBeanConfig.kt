package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.felles.rest.PingableHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

@Configuration
@ImportHttpServices(types = [EntraGraphClient::class], group = GRAPH)
class EntraClientBeanConfig {

    @Bean
    fun entraGraphAdapter(client: EntraGraphClient, norg: NorgTjeneste) = EntraGraphAdapter(client, norg)

    @Bean
    fun oidOppslag(oid: EntraOidTjeneste, cache: CacheOperations): OidOperations = EntraOidOppslag(oid, cache)

    @Bean
    fun entraProxyHealthIndicator(cfg: EntraConfig, client: EntraGraphClient) =
        PingableHealthIndicator(cfg, client::ping)
}