package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelElementer
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOppfrisker
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component

//@Component
class EntraCacheOppfrisker(private val entra: EntraTjeneste, private val oidTjeneste: AnsattOidTjeneste) : CacheOppfrisker{

    override val cacheName: String = EntraConfig.Companion.GRAPH
    private val log = getLogger(javaClass)

    override fun oppfrisk(elementer: CacheNøkkelElementer) {
        runCatching {
            val ansattId = AnsattId(elementer.id)
            when (elementer.metode) {
                "enheter" -> entra.enheter(ansattId, oidTjeneste.oidFraEntra(ansattId)!!)
                "tema" -> entra.tema(ansattId, oidTjeneste.oidFraEntra(ansattId)!!)
                else -> error("Ukjent metode ${elementer.metode} i nøkkel ${elementer.nøkkel} i cache $cacheName")
            }
            log.info("Oppfrisking av ${elementer.nøkkel} i cache $cacheName OK")
        }.getOrElse {
            log.info("Oppfrisking av ${elementer.nøkkel} i cache $cacheName etter sletting feilet, dette er ikke kritisk",it)
        }
    }
}