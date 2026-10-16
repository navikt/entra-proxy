package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.boot.conditionals.ConditionalOnProd
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Fallback
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Service

@Service
@ConditionalOnProd
class EntraMailTjeneste(
    private val client: EntraGraphClient,
    @param:Value($$"${graph.mail.avsender}") private val avsender: String) : MailSender {

    override fun send(vararg simpleMessages: SimpleMailMessage) =
        simpleMessages.forEach {
            client.sendMail(avsender, it.tilSendMailPostRequestBody())
        }
}

@Fallback
class EntraDummyMailTjeneste : MailSender {
    override fun send(vararg simpleMessages: SimpleMailMessage) {

    }
}

fun SimpleMailMessage.tilSendMailPostRequestBody() = SendMailRequest(
    message = MailMessage(
        subject = subject,
        body = MailBody(contentType = "text", content = text),
        toRecipients = to.tilMottakere(),
        ccRecipients = cc.tilMottakere(),
        bccRecipients = bcc.tilMottakere(),
        replyTo = listOfNotNull(replyTo).tilMottakere()
    ),
    saveToSentItems = false
)

private fun Array<String>?.tilMottakere() = orEmpty().asList().tilMottakere()

private fun List<String>.tilMottakere() = map { MailRecipient(MailAddress(it)) }

data class SendMailRequest(
    val message: MailMessage,
    val saveToSentItems: Boolean
)

data class MailMessage(
    val subject: String?,
    val body: MailBody,
    val toRecipients: List<MailRecipient>,
    val ccRecipients: List<MailRecipient>,
    val bccRecipients: List<MailRecipient>,
    val replyTo: List<MailRecipient>
)

data class MailBody(
    val contentType: String,
    val content: String?
)

data class MailRecipient(val emailAddress: MailAddress)

data class MailAddress(val address: String)
