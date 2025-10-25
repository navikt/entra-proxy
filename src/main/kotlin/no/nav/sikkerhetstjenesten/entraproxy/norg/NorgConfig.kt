package no.nav.sikkerhetstjenesten.entraproxy.norg
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

//@ConfigurationProperties(NORG)
class NorgConfig(
    baseUri: URI = DEFAULT_URI,
    private val enhetPath: String = DEFAULT_ENHET_PATH,
    pingPath: String = DEFAULT_PING_PATH,
    enabled: Boolean = false) : CachableRestConfig, AbstractRestConfig(baseUri, pingPath, NORG, enabled) {

    fun enhetURI(enhetsnummer: String) =
        builder().apply {
            path(enhetPath)
        }.build(enhetsnummer)

    override val navn = name
    override val varighet = Duration.ofDays(1)
    companion object {
        private val DEFAULT_URI = URI.create("http://norg2.org")
        private val DEFAULT_ENHET_PATH = "/api/vi/{enhetsnummer}"
        private const val DEFAULT_PING_PATH = "/todo"
        const val NORG = "norg"
    }

}