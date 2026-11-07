package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.felles.notifikasjon.MailRequest
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

@ClientRegistrationId(GRAPH)
interface EntraMailClient {

    @PostExchange("/users/{avsender}/sendMail")
    fun sendMail(@PathVariable avsender: String, @RequestBody request: MailRequest)
}
