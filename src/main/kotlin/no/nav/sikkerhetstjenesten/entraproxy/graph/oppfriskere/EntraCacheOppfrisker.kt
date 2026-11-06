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
            ?: return log.info("Oppfrisker ukjent id-format i nøkkel {}", nøkkelElementer)
        val metodeNavn = nøkkelElementer.metode
            ?: return log.warn("Mangler metode i nøkkel {}", nøkkelElementer)
        val metode = finnMetode(metodeNavn, argument.javaClass)
        log.info("Oppfrisker cache for nøkkel {} med metode {} og argument {}", nøkkelElementer, metode.name, argument)
        metode(entra, argument)
    }

    private fun argumentFor(id: String)  =
        AnsattId.parse(id) ?: TIdent.parse(id) ?: id.parse()

    private fun finnMetode(metodeNavn: String, argumentType: Class<*>) =
        runCatching {
            entra.javaClass.getMethod(metodeNavn, argumentType)
        }.onSuccess {
            log.info("Fant metode {} med argumenttype {} for oppfrisking", metodeNavn, argumentType.simpleName)
        }.onFailure {
            log.warn("Fant ikke metode {} med argumenttype {} for oppfrisking", metodeNavn, argumentType.simpleName, it)
        }.getOrThrow()

    private fun String.parse() =
        runCatching {
            UUID.fromString(this)
        }.getOrNull()
}


