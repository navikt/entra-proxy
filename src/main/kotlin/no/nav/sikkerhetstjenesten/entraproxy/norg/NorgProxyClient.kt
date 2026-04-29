package no.nav.sikkerhetstjenesten.entraproxy.norg

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface NorgProxyClient  {

    @GetExchange(ENHET_PATH)
    fun enhetFor(@PathVariable enhetsnummer: String): NorgEnhetRespons

    @GetExchange(PING_PATH)
    fun ping(): Any?

    companion object {
        const val ENHET_PATH = "/norg2/api/v1/enhet/{enhetsnummer}"
        const val PING_PATH = "/norg2/internal/health/liveness"
    }
}