package no.nav.sikkerhetstjenesten.entraproxy.felles.leder

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class LederVarsler(private val publisher: ApplicationEventPublisher) {

    private val gjeldendeLeder = AtomicReference<String?>(null)

    fun varsle(leder: String?) {
        val ny = leder ?: error("Kunne ikke varsle om gjeldende leder")
        val gammel = gjeldendeLeder.getAndSet(ny)
        if (gammel != ny) {
            publisher.publishEvent(LederHendelse(this, ny))
        }
    }
}
