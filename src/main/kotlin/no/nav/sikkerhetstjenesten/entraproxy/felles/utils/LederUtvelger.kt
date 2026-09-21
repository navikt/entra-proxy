package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.Disposable
import java.net.URI
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicReference

@Component
class LederUtvelger(private val cfg: LederConfig,
                    private val publisher: ApplicationEventPublisher,
                    private val sseUtvelger: SSEUtvelger,
                    private val restUtvelger: RestUtvelger) {

    private val log = getLogger(javaClass)
    private lateinit var abonnent: Disposable
    private val gjeldendeLeder = AtomicReference<String?>(null)

    private fun subscribe(uri: URI) =
        sseUtvelger.subscribe<LederUtvelgerRespons>(uri) { varsleOm(it.name) }

    private fun hent(uri: URI) =
        restUtvelger.hent<LederUtvelgerRespons>(uri, ofSeconds(5))?.name

    private fun varsleOm(leder: String?) {
        val ny = leder ?: error("Kunne ikke hente gjeldende leder fra ${cfg.get.url}")
        val gammel = gjeldendeLeder.getAndSet(ny)
        if (gammel != ny) {
            log.info("Ny leder $ny, gammel var $gammel")
            publisher.publishEvent(NyLederHendelse(this, ny))
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun klar() {
        log.info("Applikasjonen klar, lytter etter SSE-hendelser på ${cfg.sse.url}")
        abonnent = subscribe(cfg.sse.url)
        varsleOm(hent(cfg.get.url))
    }
    @EventListener(ContextClosedEvent::class)
    fun stopper() {
        log.info("Applikasjonen stopper")
        abonnent.dispose()
    }

}