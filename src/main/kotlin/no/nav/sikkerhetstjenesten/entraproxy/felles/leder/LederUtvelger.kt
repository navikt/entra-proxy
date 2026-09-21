package no.nav.sikkerhetstjenesten.entraproxy.felles.leder

import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Duration.ofSeconds

@Component
class LederUtvelger(private val sseUtvelger: SSELederUtvelger,
                    private val pollendeUtvelger: PollendeLederUtvelger,
                    private val varsler: LederVarsler) {

    private val log = getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun klar() {
        log.info("Applikasjonen klar")
        sseUtvelger.subscribe { varsler.varsle(it.name) }
        varsler.varsle(pollendeUtvelger.poll(ofSeconds(5))?.name)
    }
}