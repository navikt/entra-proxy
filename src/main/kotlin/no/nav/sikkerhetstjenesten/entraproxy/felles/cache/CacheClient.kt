package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isLocalOrTest
import org.apache.commons.pool2.impl.GenericObjectPool
import org.slf4j.LoggerFactory.getLogger
import java.time.Duration


class CacheClient(client: RedisClient, val mapper: CacheNøkkelHandler)  {
    val conn = client.connect().apply {
        timeout = Duration.ofSeconds(30)
        if (isLocalOrTest) {
            sync().configSet("notify-keyspace-events", "Exd")
        }
    }
    val log = getLogger(javaClass)

    init {
        if (isLocalOrTest) {
            conn.sync().configSet("notify-keyspace-events", "Exd")
        }
    }

    @WithSpan
    inline fun <reified T> getOne(cache: CachableConfig, id: String) =
        conn.sync().get(mapper.tilNøkkel(cache,id))?.let { json ->
            mapper.fraJson<T>(json)
        }


    @WithSpan
    fun putOne(cache: CachableConfig, id: String, value: Any, ttl: Duration)  {
        conn.async().setex(mapper.tilNøkkel(cache,id), ttl.seconds,mapper.tilJson(value))
    }

    @WithSpan
    fun getAll(cache: String) =
        conn.sync().keys("$cache::*").map {
            mapper.fraNøkkel(it)
        }.also {
            log.info("Fant ${it.size} nøkler i cache $cache")
        }


    @WithSpan
    inline fun <reified T> getMany(cache: CachableConfig, ids: Set<String>)  =
        if (ids.isEmpty()) {
            emptyMap()
        }
        else  {
            conn.sync()
                .mget(*ids.map {
                        id -> mapper.tilNøkkel(cache,id)}.toTypedArray<String>()
                )
                .filter {
                    it.hasValue()
                }
                .associate {
                    mapper.fraNøkkel(it.key) to mapper.fraJson<T>(it.value)
                }.also {
                    // tellOgLog(cache.name, it.size, ids.size)
                }
        }

    @WithSpan
    fun putMany(cache: CachableConfig, innslag: Map<String, Any>, ttl: Duration) {
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
                put(mapper.tilNøkkel(cache, key), mapper.tilJson(value))
            }
        }

}
