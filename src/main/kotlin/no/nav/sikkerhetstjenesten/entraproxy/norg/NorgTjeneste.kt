package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EnhetOperations
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.springframework.cache.annotation.Cacheable

@RestRetryingWhenRecoverableService
class NorgTjeneste(private val norg: NorgOperations) : EnhetOperations {
    @Cacheable(cacheNames = [NORG], key = "#root.methodName + ':' + #enhetnummer.verdi")
    override fun navn(enhetnummer: Enhetnummer) = norg.navn(enhetnummer)
}