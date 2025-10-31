package no.nav.sikkerhetstjenesten.entraproxy.felles.cache


import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.format
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Component
import java.util.Collections.*

@Component
class CacheAdapter(private val handler: CacheNøkkelHandler,private val client: CacheClient, private val cf: RedisConnectionFactory, cfg: CacheConfig, private vararg val cfgs: CachableRestConfig) : Pingable, MeterBinder {

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

    fun cacheStørrelser() =
        cfgs.associate {
            it.navn to "${client.cacheStørrelse(it.navn).toLong()} innslag, ttl: ${it.varighet.format()}"
        }

    override fun bindTo(registry: MeterRegistry) {
        cfgs.forEach { cfg ->
           // registry.gauge("cache.size", Tags.of("navn", cfg.navn), cf) {
           //        client.cacheStørrelse(handler.configs[cfg.navn]!!.getKeyPrefixFor(cfg.navn))
           // }
        }
    }
    companion object {
        const val VALKEY = "valkey"
    }
}

