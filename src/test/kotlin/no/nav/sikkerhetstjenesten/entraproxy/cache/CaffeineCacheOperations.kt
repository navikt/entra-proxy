package no.nav.sikkerhetstjenesten.entraproxy.cache

import com.github.benmanes.caffeine.cache.Cache
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.CacheManager
import org.springframework.cache.Cache as SpringCache
import java.time.Duration
import kotlin.reflect.KClass

class CaffeineCacheOperations(private val cacheManager: CacheManager) : CacheOperations {

    private val log = getLogger(javaClass)

    override fun getSet(nøkkel: String): Set<String> = emptySet()

    override fun putSet(nøkkel: String, verdier: Set<String>) = Unit

    override fun delete(cache: CacheNøkkelConfig, id: String): Boolean {
        val key = caffeineNøkkel(cache, id)
        val springCache = cacheManager.getCache(cache.name) ?: return false
        val existed = springCache.get(key) != null
        springCache.evict(key)
        return existed
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getOne(cache: CacheNøkkelConfig, id: String, clazz: KClass<T>): T? =
        cacheManager.getCache(cache.name)?.get(caffeineNøkkel(cache, id))?.get() as T?

    override fun putOne(cache: CacheNøkkelConfig, id: String, value: Any, ttl: Duration?) {
        cacheManager.getCache(cache.name)?.put(caffeineNøkkel(cache, id), value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getMany(cache: CacheNøkkelConfig, ids: Set<String>, clazz: KClass<T>): Map<String, T?> {
        val springCache = cacheManager.getCache(cache.name) ?: return emptyMap()
        return ids.mapNotNull { id ->
            (springCache.get(caffeineNøkkel(cache, id))?.get() as T?)?.let { id to it }
        }.toMap()
    }

    override fun putMany(cache: CacheNøkkelConfig, innslag: Map<String, Any>, ttl: Duration?) {
        val springCache = cacheManager.getCache(cache.name) ?: return
        log.trace("Caffeine bulk lagrer {} verdier for cache {}", innslag.size, cache.name)
        innslag.forEach { (id, value) -> springCache.put(caffeineNøkkel(cache, id), value) }
    }

    override fun clear(cache: CacheNøkkelConfig): Long {
        val springCache = cacheManager.getCache(cache.name) ?: return 0L
        val antall = antall(springCache, cache)
        if (cache.extraPrefix == null) {
            springCache.clear()
        } else {
            nøkler(springCache, cache).forEach(springCache::evict)
        }
        return antall
    }

    override fun sizes(vararg caches: CacheNøkkelConfig): Map<String, Long> =
        caches.associate { cache ->
            val springCache = cacheManager.getCache(cache.name) ?: error("Cache $cache ikke funnet")
            cache.fullName to antall(springCache, cache)
        }

    private fun caffeineNøkkel(cache: CacheNøkkelConfig, id: String) =
        "${cache.extraPrefix?.let { "$it:" } ?: ""}$id"

    private fun nøkler(springCache: SpringCache, cache: CacheNøkkelConfig): List<String> {
        val prefix = caffeineNøkkel(cache, "")
        return (springCache.nativeCache as Cache<*, *>).asMap().keys
            .filterIsInstance<String>()
            .filter { it.startsWith(prefix) }
    }

    private fun antall(springCache: SpringCache, cache: CacheNøkkelConfig): Long =
        if (cache.extraPrefix == null) {
            (springCache.nativeCache as Cache<*, *>).estimatedSize()
        } else {
            nøkler(springCache, cache).size.toLong()
        }
}
