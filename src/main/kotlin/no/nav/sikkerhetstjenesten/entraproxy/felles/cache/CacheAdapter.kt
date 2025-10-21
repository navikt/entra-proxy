package no.nav.sikkerhetstjenesten.entraproxy.felles.cache


import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelHandler
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.format
import org.apache.commons.pool2.impl.GenericObjectPool
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.ScanOptions.scanOptions
import org.springframework.stereotype.Component
import java.util.Collections.emptyMap

@Component
class CacheAdapter(private val handler: CacheNøkkelHandler, private val cf: RedisConnectionFactory, cfg: CacheConfig, private val pool: GenericObjectPool<*>, private vararg val cfgs: CachableRestConfig) : Pingable, MeterBinder {

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

    fun cacheSizes() = cfgs.associate { it.navn to "${cacheSize(it.navn).toLong()} innslag, ttl: ${it.varighet.format()}" }

    override fun bindTo(registry: MeterRegistry) {
        cfgs.forEach { cfg ->
            registry.gauge("cache.size", Tags.of("navn", cfg.navn), cf) {
                cacheSize((handler.configs[cfg.navn]!!.getKeyPrefixFor(cfg.navn)))
            }
        }
        registry.gauge("redis.pool.active", pool) { it.numActive.toDouble() }
        registry.gauge("redis.pool.idle", pool) { it.numIdle.toDouble() }
        registry.gauge("redis.pool.waiters", pool) { it.numWaiters.toDouble() }
        registry.gauge("redis.pool.max", pool) { it.maxTotal.toDouble() }
        registry.gauge("redis.pool.mean_borrow_wait_millis", pool) { it.meanBorrowWaitDuration.toMillis().toDouble() }
        registry.gauge("redis.pool.mean_active_time_millis", pool) { it.meanActiveDuration.toMillis().toDouble() }
    }

    private fun cacheSize(prefix: String) =
        cf.connection.use {
            it.keyCommands()
                .scan(scanOptions()
                    .match("$prefix*")
                    .count(10000)
                    .build())
                .asSequence()
                .count()
                .toDouble()
        }


    companion object {
        const val VALKEY = "valkey"
    }
}

