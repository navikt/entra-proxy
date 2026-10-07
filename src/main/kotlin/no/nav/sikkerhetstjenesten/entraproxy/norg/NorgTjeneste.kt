package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.felles.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.service.registry.ImportHttpServices

@RestRetryingWhenRecoverableService
@Service
@ImportHttpServices(types = [NorgProxyClient::class], group = NORG)
class NorgTjeneste(private val client: NorgProxyClient) {
    @Cacheable(cacheNames = [NORG],  key = "#root.methodName + ':' + #enhetnummer.verdi")
    fun navnFor(enhetnummer: Enhetnummer) = client.enhetFor(enhetnummer.verdi).navn
}