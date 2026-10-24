package no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER_CACHE
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MedlemmerCacheOppfrisker(private val entra: EntraTjeneste) : AbstractCacheOppfrisker(setOf(MEDLEMMER_CACHE)) {
    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        val gruppeId = runCatching { UUID.fromString(nøkkelElementer.id) }.getOrNull()
            ?: return log.warn("Ugyldig gruppe-id i nøkkel $nøkkelElementer")
        entra.medlemmerIGruppe(gruppeId)
    }
}
