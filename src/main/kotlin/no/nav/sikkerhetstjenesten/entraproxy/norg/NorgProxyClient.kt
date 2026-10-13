package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange

@ClientRegistrationId(NORG)
interface NorgProxyClient  {

    @GetExchange(ENHET_PATH)
    fun enhetFor(@PathVariable enhetsnummer: String): NorgEnhetRespons

    @GetExchange(PING_PATH)
    fun ping(): Any?

    companion object {
        const val NORG = "norg"
        const val ENHET_PATH = "/norg2/api/v1/enhet/{enhetsnummer}"
        const val PING_PATH = "/norg2/internal/health/liveness"
    }
}