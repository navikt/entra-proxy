package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.net.URI
import java.time.Duration

/**
 * Gjør et synkront REST-oppslag mot [uri] og blokkerer på svaret i inntil [timeout].
 */
@Component
class RestUtvelger(private val client: WebClient) {

    private val log = getLogger(javaClass)

    fun <T : Any> hent(uri: URI, type: Class<T>, timeout: Duration): T? =
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

inline fun <reified T : Any> RestUtvelger.hent(uri: URI, timeout: Duration): T? =
    hent(uri, T::class.java, timeout)
