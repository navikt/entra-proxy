package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import org.springframework.stereotype.Component
import org.springframework.web.service.registry.ImportHttpServices

@Component
@ImportHttpServices(types = [NorgProxyClient::class], group = NORG)
class NorgClientAdapter(private val client: NorgProxyClient) : NorgPort {

    override fun navnFor(enhet: Enhetnummer): String = client.enhetFor(enhet.verdi).navn
}
