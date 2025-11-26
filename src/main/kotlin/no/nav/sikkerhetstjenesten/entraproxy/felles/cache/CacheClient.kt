package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType.INTEGER
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LeaderAware
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isLocalOrTest
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.format
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.time.measureTime


@Component
class CacheClient(client: RedisClient,private vararg val cfgs: CachableRestConfig)  : LeaderAware() {
    val conn = client.connect().apply {
        timeout = Duration.ofSeconds(30)
        if (isLocalOrTest) {
            sync().configSet("notify-keyspace-events", "Exd")
        }
    }
    private val log = getLogger(javaClass)

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

    fun cacheStørrelser() =
        cfgs.associate {
            it.navn to "${cacheStørrelse(it.navn).toLong()} innslag, ttl: ${it.varighet.format()}"
        }
/**
    fun getAllCaches(cache: String) =
        conn.sync().keys("$cache::*").map {
            handler.idFraNøkkel(it)
        }.also {
            log.info("Fant ${it.size} nøkler i cache $cache")
        }

**/
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
