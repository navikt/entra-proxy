package no.nav.sikkerhetstjenesten.entraproxy.felles.leder

import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.Disposable
import java.net.URI

@Component
class SSELederUtvelger(private val client: WebClient) {

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

inline fun <reified T : Any> SSELederUtvelger.subscribe(uri: URI, noinline onNext: (T) -> Unit): Disposable =
    subscribe(uri, T::class.java, onNext)
