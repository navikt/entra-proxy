package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

@Configuration
@ImportHttpServices(
    types = [EntraGraphClient::class, EntraUserClient::class, EntraGroupClient::class, EntraMailClient::class],
    group = GRAPH)
class EntraClientBeanConfig {

    @Bean
    fun entraGraphAdapter(userClient: EntraUserClient, groupClient: EntraGroupClient, graphClient: EntraGraphClient) =
        EntraGraphAdapter(userClient, groupClient, graphClient)

    @Bean
    fun oidOppslag(oid: EntraOidTjeneste, cache: CacheOperations): EntraOidOperations =
        EntraOidOppslag(oid, cache)
}
