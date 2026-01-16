package no.nav.sikkerhetstjenesten.entraproxy.norg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class NorgTjeneste(private val adapter : NorgRestClientAdapter) {
    @WithSpan
    @Cacheable(cacheNames = [NORG],  key = "#root.methodName + ':' + #enhetnummer.verdi")
    fun navnFor(enhetnummer: Enhetnummer) = adapter.navnFor(enhetnummer.verdi)
}