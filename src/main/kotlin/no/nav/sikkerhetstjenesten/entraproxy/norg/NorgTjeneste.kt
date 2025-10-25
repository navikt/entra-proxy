package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhetsnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class NorgTjeneste(private val adapter : NorgRestClientAdapter) {
    @Cacheable(cacheNames = [NORG],  key = "#root.methodName + ':' + #enhetsnummer.verdi")
    fun navnFor(enhetsnummer: Enhetsnummer) = adapter.navnFor(enhetsnummer.verdi)
}