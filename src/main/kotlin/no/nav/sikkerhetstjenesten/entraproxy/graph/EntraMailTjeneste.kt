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
        client.sendMail(avsender, request.medGyldigInnholdstype())

    // Graph forstår kun "Text" og "HTML"; alt annet (f.eks. "JSON") vises som rå tekst
    private fun MailRequest.medGyldigInnholdstype() =
        copy(message = message.copy(body = message.body.copy(
            contentType = if (message.body.contentType.equals("text", ignoreCase = true)) "Text" else "HTML")))
        }





