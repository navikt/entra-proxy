package no.nav.sikkerhetstjenesten.entraproxy.felles.cache


import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.net.URI

private const val PONG = "pong"

@Component
class CachePingable(
    private val valkey: StringRedisTemplate,
    properties: DataRedisProperties) : Pingable {

    override val pingEndpoint = URI.create("${properties.host}:${properties.port}")
    override val name = "Cache"

    override fun ping() =
        valkey.execute { connection ->
            if (connection.ping().equals(PONG, ignoreCase = true)) {
                Unit
            } else {
                error("$name ping failed")
            }
        } ?: error("$name ping failed")
}

