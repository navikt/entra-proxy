package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient.Builder
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.Disposable
import reactor.netty.http.client.PrematureCloseException
import reactor.util.retry.Retry.backoff
import java.net.URI
import java.time.Duration.ofSeconds
import java.time.LocalDateTime
import kotlin.Long.Companion.MAX_VALUE

@Component
class LederUtvelger(private val builder: Builder,
                    @param:Value($$"${elector.get.url}") private val getUri: URI,
                    @param:Value($$"${elector.sse.url}") private val sseUri: URI,
                    private val publisher: ApplicationEventPublisher) {

    protected val log = getLogger(javaClass)
    private var subscription: Disposable? = null

    @Volatile
    private var shuttingDown = false

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        hentGjeldendeLeder()
        log.info("SSE Application ready, connecting to $sseUri")
        subscription =
            builder.build()
                .get()
                .uri(sseUri)
                .retrieve()
                .bodyToFlux<LederUtvelgerRespons>()
                .doOnError { log.error("SSE connection feilet for godt: ${it.message}", it) }
                .doOnSubscribe { log.info("SSE subscribe") }
                .doOnNext { log.info("SSE next: {} ", it) }
                .retryWhen(
                    backoff(MAX_VALUE, ofSeconds(1))
                        .maxBackoff(ofSeconds(30))
                        .filter {
                            if (shuttingDown) {
                                log.info("SSE shutdown, slutt med retries")
                                return@filter false
                            }
                            it is WebClientRequestException ||
                                    it is PrematureCloseException ||
                                    it.cause is PrematureCloseException
                        }
                        .doBeforeRetry { log.info("SSE retry ${it.failure().message}",it) }
                        .doAfterRetry { log.info("SSE connection retry etter ${it.totalRetriesInARow()} forsøk", it.failure()) }
                )
                .subscribe(
                    { publisher.publishEvent(LeaderChangedEvent(this, it.name)) },
                    { log.warn("SSE error: ${it.message}", it) }
                )
    }

    private fun hentGjeldendeLeder() {
        runCatching {
            builder.build()
                .get()
                .uri(getUri)
                .retrieve()
                .bodyToMono<LederUtvelgerRespons>()
                .block(ofSeconds(5))
        }.onSuccess { respons ->
            respons?.let {
                log.info("Hentet gjeldende leder {} ved oppstart via {}", it.name, getUri)
                publisher.publishEvent(LeaderChangedEvent(this, it.name))
            }
        }.onFailure {
            log.warn("Klarte ikke å hente gjeldende leder ved oppstart via {}: {}", getUri, it.message, it)
        }
    }

    @EventListener(ContextClosedEvent::class)
    fun onShutdown() {
        log.info("SSE Application shutting down")
        shuttingDown = true
        subscription?.dispose()
    }

    private data class LederUtvelgerRespons(val name: String, val last_update: LocalDateTime)
    class LeaderChangedEvent(source: Any, val leder: String) : ApplicationEvent(source)
}