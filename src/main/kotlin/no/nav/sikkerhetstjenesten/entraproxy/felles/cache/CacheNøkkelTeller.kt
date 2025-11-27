package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LeaderAware
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisOperations
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.time.measureTime

@Component
class CacheNøkkelTeller(private val redisTemplate: RedisOperations<String, Any?>) : LeaderAware() {
    private val log = LoggerFactory.getLogger(javaClass)

    fun tell(prefix: String) =
        somLeder(0L) {
                runBlocking {
                    var size = 0L
                    runCatching {
                        val timeUsed = measureTime {
                            size = withTimeout(Duration.ofMillis(500).toMillis()) {
                                log.info("SKraper $prefix")
                                redisTemplate.execute(DefaultRedisScript(CACHE_SIZE_SCRIPT.trimIndent(), Long::class.java),
                                    emptyList(),
                                    prefix) ?: 0L
                            }
                        }
                        log.trace("Cache størrelse oppslag fant størrelse $size på ${timeUsed.inWholeMilliseconds}ms for cache $prefix")
                        size
                    }.getOrElse { e ->
                        log.warn("Feil ved henting av størrelse for $prefix", e)
                        size
                    }
                }
            }

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