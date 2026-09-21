package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.Disposable
import java.net.URI

/**
 * Abonnerer på en Server-Sent Events (SSE)-strøm og kjører den gitte blokken for hver hendelse som mottas.
 */
@Component
class SSEUtvelger(private val client: WebClient) {

    private val log = getLogger(javaClass)

    fun <T : Any> subscribe(uri: URI, type: Class<T>, onNext: (T) -> Unit): Disposable =
        client
            .get()
            .uri(uri)
            .retrieve()
            .bodyToFlux(type)
            .subscribe(onNext) {
                log.warn("SSE feilet for {}", uri, it)
            }
}

inline fun <reified T : Any> SSEUtvelger.subscribe(uri: URI, noinline onNext: (T) -> Unit): Disposable =
    subscribe(uri, T::class.java, onNext)
