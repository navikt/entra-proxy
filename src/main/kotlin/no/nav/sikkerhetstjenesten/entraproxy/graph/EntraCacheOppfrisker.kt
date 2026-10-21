package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.ENHETER
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRUPPER_FOR_ANSATT
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.TEMA
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.UTVIDET_ANSATT
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import no.nav.sikkerhetstjenesten.felles.rest.ConsumerAwareHandlerInterceptor.Companion.USER_ID
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class EntraCacheOppfrisker(private val entra: EntraTjeneste) : AbstractCacheOppfrisker() {

    override val cacheName: String = GRAPH

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        val metode = nøkkelElementer.metode
        when (metode) {
            TEMA, ENHETER, UTVIDET_ANSATT, GRUPPER_FOR_ANSATT -> oppfriskMedMetode(nøkkelElementer, metode)
            else -> log.warn("Ukjent nøkkel $nøkkelElementer")
        }
    }


    private fun oppfriskMedMetode(elementer: CacheNøkkel, metode: String) {
        val ansattId = AnsattId(elementer.id)
        MDC.put(USER_ID, ansattId.verdi)
        runCatching {
            invoke(metode, ansattId)
        }.getOrElse {
                loggOppfriskingFeilet(elementer, it)
        }
    }

    private fun invoke(metode: String, ansattId: AnsattId) {
        when (metode) {
            TEMA -> entra.tema(ansattId)
            ENHETER -> entra.enheter(ansattId)
            UTVIDET_ANSATT -> entra.utvidetAnsatt(ansattId)
            GRUPPER_FOR_ANSATT -> entra.grupper(ansattId)
            else -> log.trace("Ukjent metode {} for ansatt {}", metode, ansattId.verdi)
        }
    }
}
