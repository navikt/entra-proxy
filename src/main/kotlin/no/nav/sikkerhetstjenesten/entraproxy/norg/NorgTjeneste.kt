package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.springframework.cache.annotation.Cacheable

@RestRetryingWhenRecoverableService
class NorgTjeneste(private val norg: NorgOperations) {
    @Cacheable(cacheNames = [NORG],  key = "#root.methodName + ':' + #enhetnummer.verdi")
    fun navn(enhetnummer: Enhetnummer) = norg.navn(enhetnummer)
}