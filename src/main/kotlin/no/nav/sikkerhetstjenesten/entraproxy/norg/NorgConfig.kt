package no.nav.sikkerhetstjenesten.entraproxy.norg
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerCachableRestConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.PING_PATH
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration
import java.time.Duration.ofHours

@Component
class NorgConfig : CachableRestConfig, AbstractRestConfig(NORG_BASE_URI, PING_PATH, NORG) {
    override val varighet = ofHours(3)
    override val navn = name
    override val caches = setOf(NORG_CACHE)

    companion object {
        val NORG_BASE_URI = URI.create("http://norg2.org")
        const val NORG = "norg"
        val NORG_CACHE = CacheNøkkelConfig(NORG)

    }

}