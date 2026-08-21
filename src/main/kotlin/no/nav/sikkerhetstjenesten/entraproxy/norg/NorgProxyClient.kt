package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
@ClientRegistrationId(NORG)
interface NorgProxyClient  {

    @GetExchange(NORG_ENHET_PATH)
    fun enhetFor(@PathVariable enhetsnummer: String): NorgEnhetRespons

    @GetExchange(NORG_PING_PATH)
    fun ping(): Any?

    companion object {
        const val NORG_ENHET_PATH = "/norg2/api/v1/enhet/{enhetsnummer}"
        const val NORG_PING_PATH = "/norg2/internal/health/liveness"
    }
}