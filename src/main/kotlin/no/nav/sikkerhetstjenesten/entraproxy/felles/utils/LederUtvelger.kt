package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.time.LocalDateTime

@Component
class LederUtvelger(private val builder: WebClient.Builder,
                    @param:Value("\${elector.sse.url}") private val uri: URI,
                    private val publisher: ApplicationEventPublisher) {

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        builder.build()
            .get()
            .uri(uri)
            .retrieve()
            .bodyToFlux(LederUtvelgerRespons::class.java)
            .subscribe {
                publisher.publishEvent(LeaderChangedEvent(this, it.name))
            }
    }

    private data class LederUtvelgerRespons(val name: String, val last_update: LocalDateTime)
    class LeaderChangedEvent(source: Any, val leder: String) : ApplicationEvent(source)
}