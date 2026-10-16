package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.microsoft.graph.models.BodyType
import com.microsoft.graph.models.EmailAddress
import com.microsoft.graph.models.ItemBody
import com.microsoft.graph.models.Message
import com.microsoft.graph.models.Recipient
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody
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

fun SimpleMailMessage.tilSendMailPostRequestBody() = SendMailPostRequestBody().apply {
    message = Message().apply {
        subject = this@tilSendMailPostRequestBody.subject
        body = ItemBody().apply {
            contentType = BodyType.Text
            content = this@tilSendMailPostRequestBody.text
        }
        toRecipients = to.tilMottakere()
        ccRecipients = cc.tilMottakere()
        bccRecipients = bcc.tilMottakere()
        replyTo = listOfNotNull(this@tilSendMailPostRequestBody.replyTo).tilMottakere()
    }
    saveToSentItems = false
}

private fun Array<String>?.tilMottakere() = orEmpty().asList().tilMottakere()

private fun List<String>.tilMottakere() = map {
    Recipient().apply { emailAddress = EmailAddress().apply { address = it } }
}
