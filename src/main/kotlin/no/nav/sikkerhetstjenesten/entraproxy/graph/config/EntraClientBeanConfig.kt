package no.nav.sikkerhetstjenesten.entraproxy.graph.config

import no.nav.sikkerhetstjenesten.entraproxy.graph.client.EntraGraphAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.EntraGraphClient
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.EntraMailClient
import no.nav.sikkerhetstjenesten.entraproxy.graph.tjeneste.EntraOidOperations
import no.nav.sikkerhetstjenesten.entraproxy.graph.tjeneste.EntraOidOppslag
import no.nav.sikkerhetstjenesten.entraproxy.graph.tjeneste.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

@Configuration
@ImportHttpServices(types = [EntraGraphClient::class, EntraMailClient::class], group = GRAPH)
class EntraClientBeanConfig {

    @Bean
    fun entraGraphAdapter(client: EntraGraphClient) =
        EntraGraphAdapter(client)

    @Bean
    fun oidOppslag(oid: EntraOidTjeneste, cache: CacheOperations): EntraOidOperations =
        EntraOidOppslag(oid, cache)
}
