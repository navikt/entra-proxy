package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange

@ClientRegistrationId(GRAPH)
interface EntraGraphClient {

    @PostExchange($$"/$batch")
    fun batch(@RequestBody request: BatchRequest): BatchRespons

    @GetExchange(ENTRA_PING_PATH)
    fun ping(): Any?

    companion object {
        const val ENTRA_PING_PATH = "/organization"
        const val GRAPH = "graph"
        internal const val TOP = 999
    }
}
