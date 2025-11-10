package no.nav.sikkerhetstjenesten.entraproxy.felles.cache


import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Component
import java.util.Collections.*

@Component
class CachePingable(private val cf: RedisConnectionFactory, cfg: CacheConfig) : Pingable {

    override val pingEndpoint  =  "${cfg.host}:${cfg.port}"
    override val name = "Cache"

    override fun ping() =
        cf.connection.use {
            if (it.ping().equals("PONG", ignoreCase = true)) {
                emptyMap<String,String>()
            }
            else {
                error("$name ping failed")
            }
        }
}

