package no.nav.sikkerhetstjenesten.entraproxy.felles.leder

import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.net.URI
import java.time.Duration

@Component
class PollendeLederUtvelger(private val client: WebClient, @Value($$"${elector.get.url}") private val uri: URI) {

    private val log = getLogger(javaClass)

    fun poll(timeout: Duration = Duration.ofSeconds(5)) =
        runCatching {
            client
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono<LederUtvelgerRespons>()
                .block(timeout)
        }.onFailure {
            log.warn("Klarte ikke å hente leder fra {}", uri, it)
        }.getOrThrow()
}
