package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.felles.notifikasjon.MailRequest
import no.nav.sikkerhetstjenesten.felles.notifikasjon.MailRequest.MailMessage
import no.nav.sikkerhetstjenesten.felles.notifikasjon.MailRequest.MailMessage.MailBody
import no.nav.sikkerhetstjenesten.felles.notifikasjon.MailRequest.MailMessage.MailRecipient
import no.nav.sikkerhetstjenesten.felles.notifikasjon.MailRequest.MailMessage.MailRecipient.MailAddress
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Service

@Service
class EntraMailTjeneste(
    private val client: EntraGraphClient,
    @param:Value($$"${graph.mail.avsender}") private val avsender: String) : MailSender {

    private val log = getLogger(javaClass)


    override fun send(vararg simpleMessages: SimpleMailMessage) =
        simpleMessages.forEach {
            log.info("Sender mail $it")
            client.sendMail(avsender, it.body())
        }
}

fun SimpleMailMessage.body() = MailRequest(
    MailMessage(subject, MailBody("html", text),
        to.tilMottakere(),
        cc.tilMottakere(),
        bcc.tilMottakere(),
        listOfNotNull(replyTo).tilMottakere()
    ), false)

private fun Array<String>?.tilMottakere() = orEmpty().asList().tilMottakere()

private fun List<String>.tilMottakere() = map { MailRecipient(MailAddress(it)) }




