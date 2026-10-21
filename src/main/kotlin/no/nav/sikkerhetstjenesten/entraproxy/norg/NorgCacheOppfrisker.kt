package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import no.nav.sikkerhetstjenesten.felles.cache.AbstractCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import org.springframework.stereotype.Component

@Component
class NorgCacheOppfrisker(private val norg: NorgTjeneste) : AbstractCacheOppfrisker() {

    override val cacheName: String = NORG

    override fun doOppfrisk(nøkkelElementer: CacheNøkkel) {
        val enhetnummer = nøkkelElementer.id.somEnhetnummer()
            ?: return log.warn("Ukjent id-format i nøkkel $nøkkelElementer")
        runCatching {
            norg.navn(enhetnummer)
        }.onFailure {
            loggOppfriskingFeilet(nøkkelElementer, it)
        }
    }

    private fun String.somEnhetnummer() = runCatching { Enhetnummer(this) }.getOrNull()
}
