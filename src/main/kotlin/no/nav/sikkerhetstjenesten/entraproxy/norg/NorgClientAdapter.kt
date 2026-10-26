package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import no.nav.sikkerhetstjenesten.felles.rest.PingableHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.service.registry.ImportHttpServices

@Component
@ImportHttpServices(types = [NorgProxyClient::class], group = NORG)
class NorgClientAdapter(private val client: NorgProxyClient) : NorgOperations {

    override fun navn(enhet: Enhetnummer): String = client.enhetFor(enhet.verdi).navn

    @Bean
    fun norgHealthIndicator(cfg: NorgConfig) = PingableHealthIndicator(cfg, client::ping)
}
