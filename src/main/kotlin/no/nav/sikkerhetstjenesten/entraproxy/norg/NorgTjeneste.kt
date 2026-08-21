package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverableService
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.service.registry.ImportHttpServices

@RetryingWhenRecoverableService
@ImportHttpServices(group = NORG, types = [NorgProxyClient::class])
class NorgTjeneste(private val client: NorgProxyClient) {
    @Cacheable(cacheNames = [NORG],  key = "#root.methodName + ':' + #enhetnummer.verdi")
    fun navnFor(enhetnummer: Enhetnummer) = client.enhetFor(enhetnummer.verdi).navn

}