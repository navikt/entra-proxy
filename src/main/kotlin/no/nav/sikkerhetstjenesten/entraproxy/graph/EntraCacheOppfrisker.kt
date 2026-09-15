package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkel
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.ConsumerAwareHandlerInterceptor.Companion.USER_ID
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.ENHETER
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRUPPER_FOR_ANSATT
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.TEMA
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.UTVIDET_ANSATT
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EntraCacheOppfrisker(private val entra: EntraTjeneste, private val oidTjeneste: EntraOidTjeneste, private val cache: CacheOperations) : AbstractCacheOppfrisker() {

    override val cacheName: String = GRAPH

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) =
        if (nøkkelElementer.metode == TEMA || nøkkelElementer.metode == ENHETER ||nøkkelElementer.metode == UTVIDET_ANSATT || nøkkelElementer.metode == GRUPPER_FOR_ANSATT
    )
            oppfriskMedMetode(nøkkelElementer, nøkkelElementer.metode)
        else
            log.warn("Ukjent nøkkel $nøkkelElementer")


    private fun oppfriskMedMetode(elementer: CacheNøkkel, metode: String) {
        val ansattId = AnsattId(elementer.id)
        MDC.put(USER_ID, ansattId.verdi)
        runCatching {
            var oid  = oidTjeneste.ansattOid(ansattId)
            if (oid == null) {
                log.info("INgen oid i cache for ansatt $ansattId, henter på nytt fra Entra og oppfrisker OID-cache")
                cache.delete(OID_CACHE,elementer.id)
                oid  = oidTjeneste.ansattOid(ansattId)
            }
            if (oid != null) {
                invoke(metode, ansattId, oid)
            }
            else {
                throw IllegalStateException("Kunne ikke finne oid for ansatt $ansattId, kan ikke oppfriske cache for $metode")
            }
        }.getOrElse {
                loggOppfriskingFeilet(elementer, it)
        }
    }

    private fun invoke(metode: String, ansattId: AnsattId, oid: UUID) {
        when (metode) {
            TEMA -> entra.tema(ansattId, oid)
            ENHETER -> entra.enheter(ansattId, oid)
            UTVIDET_ANSATT -> entra.utvidetAnsatt(ansattId)
            GRUPPER_FOR_ANSATT -> entra.grupperForAnsatt(ansattId, oid)
            else -> log.trace("Ukjent metode {} for ansatt {} og oid {}", metode, ansattId.verdi, oid)
        }
    }
}