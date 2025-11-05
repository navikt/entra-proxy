package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType.INTEGER
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LeaderAware
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isLocalOrTest
import org.slf4j.LoggerFactory.getLogger
import java.time.Duration
import kotlin.time.measureTime


class CacheClient(client: RedisClient, val mapper: CacheNøkkelHandler)  : LeaderAware(){
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



    fun cacheStørrelse(prefix: String): Double {
        if (!erLeder) return 0.0
        return runBlocking {
            runCatching {
                var size = 0.0
                val timeUsed = measureTime {
                    size = withTimeout(Duration.ofSeconds(1).toMillis()) {
                        conn.sync().eval<Int>(CACHE_SIZE_SCRIPT, INTEGER, emptyArray(), prefix).toDouble()
                    }
                }
                log.info("Cache størrelse oppslag fant størrelse ${size.toLong()} på ${timeUsed.inWholeMilliseconds}ms for cache $prefix")
                size
            }.getOrElse { e ->
                log.warn("Feil ved henting av størrelse for $prefix", e)
                0.0
            }
        }
    }


    data class CachableConfig(val name: String, val extraPrefix: String? = null)

    companion object {
        private const val CACHE_SIZE_SCRIPT = """
    local cursor = "0"
    local count = 0
    local prefix = ARGV[1]
    repeat
        local result = redis.call("SCAN", cursor, "MATCH", prefix .. "*", "COUNT", 10000)
        cursor = result[1]
        local keys = result[2]
        count = count + #keys
    until cursor == "0"
    return count
"""
    }
}
