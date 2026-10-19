package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.felles.notifikasjon.MailRequest
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class EntraMailTjeneste(
    private val client: EntraGraphClient,
    @param:Value($$"${graph.mail.avsender}") private val avsender: String)  {

    private val log = getLogger(javaClass)


    fun send(request: MailRequest) =
        client.sendMail(avsender, request)
}






