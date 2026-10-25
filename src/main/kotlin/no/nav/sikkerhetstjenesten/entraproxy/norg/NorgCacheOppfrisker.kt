package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer.Companion.parse
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import org.springframework.stereotype.Component

@Component
class NorgCacheOppfrisker(private val norg: NorgTjeneste, cfg: NorgConfig) : AbstractCacheOppfrisker(cfg) {

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        parse(nøkkelElementer.id)?.let { enhetnummer ->
            norg.navn(enhetnummer)
        } ?: log.warn("Ukjent id-format i nøkkel $nøkkelElementer")
    }
}
