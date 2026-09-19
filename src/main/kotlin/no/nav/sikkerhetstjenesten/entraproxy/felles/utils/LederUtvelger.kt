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
                    private val elector: ElectorConfig,
                    private val publisher: ApplicationEventPublisher) {

    protected val log = getLogger(javaClass)
    private lateinit var subscription: Disposable
    private val gjeldendeLeder = AtomicReference<String?>(null)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        log.info("SSE Application ready,connecting to ${elector.sse.url}")
        subscription = subscribeSSE()
        hentGjeldendeLeder()
    }

    private fun subscribeSSE() =
        client
            .get()
            .uri(elector.sse.url)
            .retrieve()
            .bodyToFlux<LederUtvelgerRespons>()
            .subscribe(
                {
                    varsleOmLeder(it.name)
                }, {
                    log.warn("SSE error: ${it.message}", it)
                }
            )

    private fun hentGjeldendeLeder() {
        runCatching {
            client
                .get()
                .uri(elector.get.url)
                .retrieve()
                .bodyToMono<LederUtvelgerRespons>()
                .block(ofSeconds(5))
        }.onSuccess { respons ->
            respons?.let {
                varsleOmLeder(it.name)
            }
        }.onFailure {
            log.warn("Klarte ikke å hente gjeldende leder via {}", elector.get.url,  it)
        }
    }

    private fun varsleOmLeder(ny: String) {
        val gammel = gjeldendeLeder.getAndSet(ny)
        if ( gammel != ny) {
            log.info("Ny leder: {}, gammel var {}", ny,gammel)
            publisher.publishEvent(LeaderChangedEvent(this, ny))
        }
    }

    @EventListener(ContextClosedEvent::class)
    fun onShutdown() {
        log.info("SSE Application shutting down")
        subscription.dispose()
    }

    private data class LederUtvelgerRespons(val name: String, val last_update: LocalDateTime)
    class LeaderChangedEvent(source: Any, val leder: String) : ApplicationEvent(source)
}