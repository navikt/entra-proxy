package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.stereotype.Component

@Component
class CacheMeteredErrorHandler(private val registry: MeterRegistry) : CacheErrorHandler {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun handleCacheGetError(e: RuntimeException, cache: Cache, key: Any) =
        record("get", cache, e)

    override fun handleCachePutError(e: RuntimeException, cache: Cache, key: Any, value: Any?) =
        record("put", cache, e)

    override fun handleCacheEvictError(e: RuntimeException, cache: Cache, key: Any) =
        record("evict", cache, e).also {
            throw e
        }

    override fun handleCacheClearError(e: RuntimeException, cache: Cache) =
        record("clear", cache, e).also {
            throw e
        }

    private fun record(op: String, cache: Cache, e: RuntimeException) {
        registry.counter("cache.operation.failed", "op", op, "cache", cache.name,
            "exception", e.javaClass.simpleName).increment()
        log.warn("Cache $op feilet for ${cache.name}: ${e.message}")
    }
}