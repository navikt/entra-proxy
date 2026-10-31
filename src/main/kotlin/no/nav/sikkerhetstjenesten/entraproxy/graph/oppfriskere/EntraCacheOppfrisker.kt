package no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EntraCacheOppfrisker(private val entra: EntraTjeneste, cfg: EntraConfig) : AbstractCacheOppfrisker(cfg) {

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        val argument = argumentFor(nøkkelElementer.id)
            ?: return log.info("Oppfrisker ukjent id-format i nøkkel $nøkkelElementer")
        val metode = finnMetode(nøkkelElementer.metode!!, argument.javaClass)
            ?: return log.info("Oppfrisker ukjent nøkkel $nøkkelElementer")
        log.info("Oppfrisker cache for nøkkel $nøkkelElementer med metode ${metode.name} og argument $argument")
        metode(entra, argument).also {
            log.info("Oppfrisket cache for entra ${metode.name} med argument ${argument.javaClass.simpleName}")
        }
    }

    private fun argumentFor(id: String)  =
        AnsattId.parse(id) ?: TIdent.parse(id) ?: id.parse()

    private fun finnMetode(metodeNavn: String, argumentType: Class<*>) =
        runCatching {
            entra.javaClass.getMethod(metodeNavn, argumentType)
        }.onSuccess {
            log.info("Fant metode $metodeNavn med argumenttype ${argumentType.simpleName} for oppfrisking")
        }.onFailure {
            log.warn("Fant ikke metode $metodeNavn med argumenttype ${argumentType.simpleName} for oppfrisking", it)
        }.getOrThrow()

    private fun String.parse() =
        runCatching {
            UUID.fromString(this)
        }.getOrNull()
}


