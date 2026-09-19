package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.Disposable
import java.time.Duration.ofSeconds
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

@Component
class LederUtvelger(private val client: WebClient,
                    private val config: ElectorConfig,
                    private val publisher: ApplicationEventPublisher) {

    protected val log = getLogger(javaClass)
    private lateinit var subscription: Disposable
    private val gjeldendeLeder = AtomicReference<String?>(null)

    @EventListener(ApplicationReadyEvent::class)
    fun klar() {
        log.info("Applikasjonen klar, lytter etter SSE-hendelser på  ${config.sse.url}")
        subscription = abonner()
        hentGjeldendeLeder()
    }
    @EventListener(ContextClosedEvent::class)
    fun stopper() {
        log.info("Applikasjonen stopper")
        subscription.dispose()
    }

    private fun abonner() =
        client
            .get()
            .uri(config.sse.url)
            .retrieve()
            .bodyToFlux<LederUtvelgerRespons>()
            .subscribe(
                {
                    varsleOm(it.name)
                }, {
                    log.warn("SSE feilet", it)
                }
            )

    private fun hentGjeldendeLeder() {
        runCatching {
            client
                .get()
                .uri(config.get.url)
                .retrieve()
                .bodyToMono<LederUtvelgerRespons>()
                .block(ofSeconds(5))
        }.onSuccess { respons ->
            respons?.let {
                varsleOm(it.name)
            }
        }.onFailure {
            log.warn("Klarte ikke å hente gjeldende leder via {}", config.get.url,  it)
        }
    }

    private fun varsleOm(leder: String) {
        val gammelLeder = gjeldendeLeder.getAndSet(leder)
        if (gammelLeder != leder) {
            log.info("Ny leder $leder, gammel var $gammelLeder")
            publisher.publishEvent(LeaderChangedEvent(this, leder))
        }
    }



    private data class LederUtvelgerRespons(val name: String, val last_update: LocalDateTime)
    class LeaderChangedEvent(source: Any, val leder: String) : ApplicationEvent(source)
}