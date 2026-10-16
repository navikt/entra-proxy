package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.boot.conditionals.ConditionalOnProd
import org.slf4j.LoggerFactory
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Component

@Component
@ConditionalOnProd
class EntraMailOppstartsTest(
    private val mailSender: MailSender,
    @param:Value($$"${graph.mail.avsender}") private val avsender: String
) {
    private val log = getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun sendTestmelding() {
        try {
            mailSender.send(SimpleMailMessage().apply {
                from = avsender
                setTo("jan-olav.eide@nav.no")
                subject = "OVERSTYRING"
                text = "Testmelding sendt ved oppstart"
            })
            log.info("Testmelding sendt ved oppstart")
        } catch (e: Exception) {
            log.error("Kunne ikke sende testmelding ved oppstart", e)
        }
    }
}
