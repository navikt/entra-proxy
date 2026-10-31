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
        val metode = finnMetode(nøkkelElementer.metode, argument.javaClass)
            ?: return log.info("Oppfrisker ukjent nøkkel $nøkkelElementer")
        log.info("Oppfrisker cache for nøkkel $nøkkelElementer med metode ${metode.name} og argument $argument")
        metode(entra, argument)
    }

    private fun argumentFor(id: String): Any? =
        AnsattId.parse(id) ?: TIdent.parse(id) ?: id.parse()

    private fun finnMetode(navn: String?, argumentType: Class<*>) =
        navn?.let {
            runCatching {
                entra.javaClass.getMethod(it, argumentType)
            }.getOrNull()
        }

    private fun String.parse() =
        runCatching {
            UUID.fromString(this)
        }.getOrNull()
}


