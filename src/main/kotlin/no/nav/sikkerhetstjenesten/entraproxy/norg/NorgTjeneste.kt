package no.nav.sikkerhetstjenesten.entraproxy.norg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.service.registry.ImportHttpServices

@RetryingWhenRecoverable
@Service
@ImportHttpServices(types = [NorgProxyClient::class], group = NORG)
class NorgTjeneste(private val client: NorgProxyClient) {
    @WithSpan
    @Cacheable(cacheNames = [NORG],  key = "#root.methodName + ':' + #enhetnummer.verdi")
    fun navnFor(enhetnummer: Enhetnummer) = client.enhetFor(enhetnummer.verdi).navn

}