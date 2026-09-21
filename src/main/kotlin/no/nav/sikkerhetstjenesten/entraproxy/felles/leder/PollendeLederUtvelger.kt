package no.nav.sikkerhetstjenesten.entraproxy.felles.leder

import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.time.Duration

/**
 * Gjør et synkront REST-oppslag mot [uri] og blokkerer på svaret i inntil [timeout].
 */
@Component
class PollendeLederUtvelger(private val client: WebClient) {

    private val log = getLogger(javaClass)

    fun <T : Any> poll(uri: URI, type: Class<T>, timeout: Duration): T? =
        runCatching {
            client
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(type)
                .block(timeout)
        }.onFailure {
            log.warn("Klarte ikke å hente fra {}", uri, it)
        }.getOrThrow()
}

inline fun <reified T : Any> PollendeLederUtvelger.poll(uri: URI, timeout: Duration): T? =
    poll(uri, T::class.java, timeout)
