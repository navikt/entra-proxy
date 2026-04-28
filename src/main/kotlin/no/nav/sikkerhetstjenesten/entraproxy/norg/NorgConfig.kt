package no.nav.sikkerhetstjenesten.entraproxy.norg
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.ENHET_PATH
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.PING_PATH
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration

@Component
class NorgConfig : CachableRestConfig, AbstractRestConfig(BASE_URI, PING_PATH, NORG) {
    override val varighet = Duration.ofHours(3)
    override val navn = name

    companion object {
        val BASE_URI = URI.create("http://norg2.org")
        const val NORG = "norg"
    }

}