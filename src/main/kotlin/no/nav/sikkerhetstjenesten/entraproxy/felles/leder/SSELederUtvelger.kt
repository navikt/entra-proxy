package no.nav.sikkerhetstjenesten.entraproxy.felles.leder

import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.Disposable
import java.net.URI

@Component
class SSELederUtvelger(private val client: WebClient, @Value($$"${elector.sse.url}") private val uri: URI) {

    private val log = getLogger(javaClass)
    private lateinit var abonnent: Disposable

    fun subscribe(onNext: (LederUtvelgerRespons) -> Unit) {
        abonnent = client
            .get()
            .uri(uri)
            .retrieve()
            .bodyToFlux<LederUtvelgerRespons>()
            .subscribe(onNext) {
                log.warn("SSE feilet for {}", uri, it)
            }
    }

    @EventListener(ContextClosedEvent::class)
    fun stopper() {
        log.info("Applikasjonen stopper, avslutter SSE-abonnement")
        abonnent.dispose()
    }
}
