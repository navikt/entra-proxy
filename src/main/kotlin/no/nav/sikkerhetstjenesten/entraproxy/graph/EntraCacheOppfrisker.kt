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
        val oid  = oidTjeneste.ansattOid(ansattId)
        runCatching {
            invoke(metode, ansattId, oid)
        }.getOrElse {
            if (it is IrrecoverableRestException && it.statusCode == NOT_FOUND) {
                log.warn("Ansatt {} med oid {} ikke funnet i Entra, sletter og refresher cache entry", ansattId.verdi, oid)
                cache.delete(elementer.id,OID_CACHE)
                val nyoid = oidTjeneste.ansattOid(ansattId)
                log.info("Refresh oid OK for ansatt {}, ny verdi er {}", ansattId.verdi, nyoid)
                invoke(metode, ansattId, nyoid)
            }
            else {
                loggOppfriskingFeilet(elementer, it)
            }
        }
    }

    private fun invoke(metode: String, ansattId: AnsattId, oid: UUID) {
        when (metode) {
            TEMA -> entra.tema(ansattId, oid)
            ENHETER -> entra.enheter(ansattId, oid)
            else -> log.warn("Ukjent metode $metode for ansatt ${ansattId.verdi} og oid $oid")
        }
    }

    companion object {
        private const val TEMA = "tema"
        private const val ENHETER = "enheter"
    }
}