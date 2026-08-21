package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RestConfig
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG_PING_PATH
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration

@Component
class NorgConfig(@Value("\${spring.http.serviceclient.norg.base-url}") baseUrl: URI,
    @param:Value("\${norg.varighet:3h}") override val varighet: Duration) : CachableRestConfig, RestConfig(baseUrl, NORG_PING_PATH, NORG) {
    override val navn = NORG
    override val caches = setOf(NORG_CACHE)

    companion object {
        val NORG_CACHE = CacheNøkkelConfig(NORG, NAVN_FOR_CACHE)
        const val NORG = "norg"
        const val NAVN_FOR_CACHE = "navnFor"
    }
}