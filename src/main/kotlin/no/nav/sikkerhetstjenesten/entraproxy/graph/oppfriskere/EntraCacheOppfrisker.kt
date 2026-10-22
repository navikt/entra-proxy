package no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import no.nav.sikkerhetstjenesten.felles.rest.ConsumerAwareHandlerInterceptor.Companion.USER_ID
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class EntraCacheOppfrisker(private val entra: EntraTjeneste) : AbstractCacheOppfrisker() {

    override val cacheName: String = GRAPH

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        val argument = argumentFor(nøkkelElementer.id)
            ?: return log.info("Ukjent id-format i nøkkel $nøkkelElementer")
        val metode = finnMetode(nøkkelElementer.metode, argument.javaClass)
            ?: return log.warn("Ukjent nøkkel $nøkkelElementer")
        MDC.put(USER_ID, nøkkelElementer.id)
        runCatching {
            metode.invoke(entra, argument)
        }.onFailure {
            loggOppfriskingFeilet(nøkkelElementer, it)
        }
    }

    private fun argumentFor(id: String): Any? =
        runCatching { AnsattId(id) }.getOrNull()
            ?: runCatching { TIdent(id) }.getOrNull()

    private fun finnMetode(navn: String?, argumentType: Class<*>) =
        navn?.let { runCatching { entra.javaClass.getMethod(it, argumentType) }.getOrNull() }
}
