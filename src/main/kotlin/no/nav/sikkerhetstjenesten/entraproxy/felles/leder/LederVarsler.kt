package no.nav.sikkerhetstjenesten.entraproxy.felles.leder

import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class LederVarsler(private val publisher: ApplicationEventPublisher) {

    private val log = getLogger(javaClass)
    private val gjeldendeLeder = AtomicReference<String?>(null)

    fun varsle(leder: String?) {
        val ny = leder ?: error("Kunne ikke hente gjeldende leder")
        val gammel = gjeldendeLeder.getAndSet(ny)
        if (gammel != ny) {
            log.info("Ny leder $ny, gammel var $gammel")
            publisher.publishEvent(LederHendelse(this, ny))
        }
    }
}
