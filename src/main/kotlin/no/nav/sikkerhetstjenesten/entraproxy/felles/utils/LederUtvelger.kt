package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.Disposable
import java.net.URI
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicReference

@Component
class LederUtvelger(private val client: WebClient,
                    private val cfg: LederConfig,
                    private val publisher: ApplicationEventPublisher) {

    protected val log = getLogger(javaClass)
    private lateinit var abonnent: Disposable
    private val gjeldendeLeder = AtomicReference<String?>(null)

    private fun abonnerPå(uri: URI) =
        client
            .get()
            .uri(uri)
            .retrieve()
            .bodyToFlux<LederUtvelgerRespons>()
            .subscribe({
                    varsleOm(it.name)
                }, {
                    log.warn("SSE feilet", it)
                })

    private fun gjeldendeLederFra(uri: URI) =
        runCatching {
            client
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono<LederUtvelgerRespons>()
                .block(ofSeconds(5))
                ?.name
        }.onFailure {
            log.warn("Klarte ikke å hente gjeldende leder via {}", uri, it)
        }.getOrThrow()

    private fun varsleOm(leder: String?) {
        leder?.let { ny ->
            val gammel = gjeldendeLeder.getAndSet(ny)
            if (gammel != ny) {
                log.info("Ny leder $ny, gammel var $gammel")
                publisher.publishEvent(NyLederHendelse(this, ny ))
            }
        }?: error("Kunne ikke hente gjeldende leder fra ${cfg.get.url}")
    }

    @EventListener(ApplicationReadyEvent::class)
    fun klar() {
        log.info("Applikasjonen klar, lytter etter SSE-hendelser på ${cfg.sse.url}")
        abonnent = abonnerPå(cfg.sse.url)
        varsleOm(gjeldendeLederFra(cfg.get.url))
    }
    @EventListener(ContextClosedEvent::class)
    fun stopper() {
        log.info("Applikasjonen stopper")
        abonnent.dispose()
    }

}