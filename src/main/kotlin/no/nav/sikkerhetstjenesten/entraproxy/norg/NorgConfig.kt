package no.nav.sikkerhetstjenesten.entraproxy.norg
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.PING_PATH
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration.ofHours

@Component
class NorgConfig(@Value($$"${spring.http.serviceclient.norg.base-url}") baseUrl: URI) : CachableRestConfig, AbstractRestConfig(baseUrl, PING_PATH, NORG) {
    override val varighet = ofHours(3)
    override val navn = name
    override val caches = setOf(CacheNøkkelConfig(NORG))

}