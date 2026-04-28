package no.nav.sikkerhetstjenesten.entraproxy.norg

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.PingableHealthIndicator
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@RetryingWhenRecoverable
@Service
class NorgTjeneste(private val client: NorgProxyClient, private val cfg: NorgConfig) : Pingable {
    @WithSpan
    @Cacheable(cacheNames = [NORG],  key = "#root.methodName + ':' + #enhetnummer.verdi")
    fun navnFor(enhetnummer: Enhetnummer) = client.navnFor(enhetnummer.verdi).navn
    override fun ping() = client.ping()

    override val pingEndpoint = cfg.pingEndpoint.toString()
    override val name = NORG
}