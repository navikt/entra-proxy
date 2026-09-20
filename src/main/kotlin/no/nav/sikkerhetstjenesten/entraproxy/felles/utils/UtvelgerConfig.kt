package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "elector")
data class UtvelgerConfig(val get: Endpoint, val sse: Endpoint) {
    data class Endpoint(val url: URI)
}
