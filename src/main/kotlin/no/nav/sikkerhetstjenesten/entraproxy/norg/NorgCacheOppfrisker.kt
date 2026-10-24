package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer.Companion.parse
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import org.springframework.stereotype.Component

@Component
class NorgCacheOppfrisker(private val norg: NorgTjeneste) : AbstractCacheOppfrisker(NORG) {

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        val enhetnummer = parse(nøkkelElementer.id)
            ?: return log.warn("Ukjent id-format i nøkkel $nøkkelElementer")
        runCatching {
            norg.navn(enhetnummer)
        }.onFailure {
            loggOppfriskingFeilet(nøkkelElementer, it)
        }
    }

}
