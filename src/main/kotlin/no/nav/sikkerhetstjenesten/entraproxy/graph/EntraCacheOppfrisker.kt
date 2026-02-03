package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheClient
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelElementer
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOppfriskerTeller
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.ConsumerAwareHandlerInterceptor.Companion.USER_ID
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.IrrecoverableRestException
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.OID_CACHE
import org.slf4j.MDC
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EntraCacheOppfrisker(private val entra: EntraTjeneste, private val oidTjeneste: EntraOidTjeneste, private val cache: CacheClient, private val teller: CacheOppfriskerTeller) : AbstractCacheOppfrisker() {

    override val cacheName: String = GRAPH

    override fun doOppfrisk(nøkkelElementer: CacheNøkkelElementer) =
        if (nøkkelElementer.metode == TEMA || nøkkelElementer.metode == ENHETER)
            oppfriskMedMetode(nøkkelElementer, nøkkelElementer.metode)
        else
            log.warn("Ukjent nøkkel $nøkkelElementer")


    private fun oppfriskMedMetode(elementer: CacheNøkkelElementer, metode: String) {
        val ansattId = AnsattId(elementer.id)
        MDC.put(USER_ID, ansattId.verdi)
        runCatching {
            var oid  = oidTjeneste.ansattOid(ansattId)
            if (oid == null) {
                log.info("INgen oid i cache for ansatt $ansattId, henter på nytt fra Entra og oppfrisker OID-cache")
                cache.delete(elementer.id,OID_CACHE)
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
            else -> log.trace("Ukjent metode {} for ansatt {} og oid {}", metode, ansattId.verdi, oid)
        }
    }

    companion object {
        private const val TEMA = "tema"
        private const val ENHETER = "enheter"
    }
}