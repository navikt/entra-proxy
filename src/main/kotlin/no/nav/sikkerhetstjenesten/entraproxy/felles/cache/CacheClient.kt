package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.lettuce.core.RedisClient
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LeaderAware
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isLocalOrTest
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.format
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Duration.ofSeconds
import kotlin.reflect.KClass


@Component
class CacheClient(client: RedisClient, private val adapter : CacheStørrelseAdapter,  val handler: CacheNøkkelHandler,vararg val cfgs: CachableRestConfig) : LeaderAware(), CacheOperations {
    private val conn = client.connect().apply {
        timeout = ofSeconds(30)
        if (isLocalOrTest) {
            sync().configSet("notify-keyspace-events", "Exd")
        }
    }

    private val log = getLogger(javaClass)


    override fun tilNøkkel(cache: CachableConfig, id: String) = handler.tilNøkkel(cache, id)


    @WithSpan
    override fun delete(cache: CachableConfig, id: String) =
        conn.sync().del(handler.tilNøkkel(cache, id))


    @WithSpan
    override fun <T : Any> getOne(cache: CachableConfig,id: String, clazz: KClass<T>): T? =
        conn.sync().get(handler.tilNøkkel(cache, id))?.let { json ->
            handler.fraJson(json, clazz)
        }

    @WithSpan
    override fun putOne(cache: CachableConfig, id: String, value: Any, ttl: Duration) {
        conn.async().setex(handler.tilNøkkel(cache, id), ttl.seconds, handler.tilJson(value))
    }


    @WithSpan
    override fun <T : Any> getMany(cache: CachableConfig, ids: Set<String>, clazz: KClass<T>): Map<String, T> =
        if (ids.isEmpty()) {
            emptyMap()
        } else {
            conn.sync()
                .mget(*ids.map { id -> handler.tilNøkkel(cache, id) }.toTypedArray<String>())
                .filter { it.hasValue() }
                .associate { handler.idFraNøkkel(it.key) to handler.fraJson(it.value, clazz) }

        }

    @WithSpan
    override fun putMany(cache: CachableConfig, innslag: Map<String, Any>, ttl: Duration) {
        if (innslag.isNotEmpty()) {
            log.trace("Bulk lagrer {} verdier for cache {} med prefix {}", innslag.size, cache.name, cache.extraPrefix)
            conn.apply {
                with(payloadFor(innslag, cache)) {
                    setAutoFlushCommands(false)
                    async().mset(this)
                    keys.forEach { key ->
                        async().expire(key, ttl.seconds)
                    }
                }
                flushCommands()
                setAutoFlushCommands(true)
            }
        }
    }

    private fun payloadFor(innslag: Map<String, Any>, cache: CachableConfig) =
        buildMap {
            innslag.forEach { (key, value) ->
                put(handler.tilNøkkel(cache, key), handler.tilJson(value))
            }
        }


    fun cacheStørrelser() =
        cfgs.associate {
            it.navn to "${cacheStørrelse(it.navn)} innslag, ttl: ${it.varighet.format()}"
        }
    fun cacheStørrelse(cache: String)  =
        somLeder(0L,"henting av cache størrelse for $cache") {
            adapter.størrelse(cache)
        }
}

data class CachableConfig(val name: String, val extraPrefix: String? = null)