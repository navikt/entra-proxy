package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.CacheManager
import java.time.Duration
import kotlin.reflect.KClass

class CaffeineCacheOperations(private val cacheManager: CacheManager) : CacheOperations {

    private val log = getLogger(javaClass)

    override fun delete(cache: CacheNøkkelConfig, id: String) : Boolean {
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
        if (ids.isEmpty()) return emptyMap()
        val springCache = cacheManager.getCache(cache.name) ?: return emptyMap()
        return ids.associateWith { id ->
            springCache.get(caffeineNøkkel(cache, id))?.get() as T?
        }.filterValues { it != null }
    }

    override fun putMany(cache: CacheNøkkelConfig, innslag: Map<String, Any>, ttl: Duration?) {
        val springCache = cacheManager.getCache(cache.name) ?: return
        log.trace("Caffeine bulk lagrer {} verdier for cache {}", innslag.size, cache.name)
        innslag.forEach { (id, value) -> springCache.put(caffeineNøkkel(cache, id), value) }
    }

    private fun caffeineNøkkel(cache: CacheNøkkelConfig, id: String): String {
        val extra = cache.extraPrefix?.let { "$it:" } ?: ""
        return "$extra$id"
    }
    override fun clear(cache: CacheNøkkelConfig): Long {
        val springCache = cacheManager.getCache(cache.name) ?: return 0L
        val nativeCache = springCache.nativeCache as com.github.benmanes.caffeine.cache.Cache<*, *>
        return if (cache.extraPrefix == null) {
            val deleted = nativeCache.estimatedSize()
            springCache.clear()
            deleted
        } else {
            val prefix = caffeineNøkkel(cache, "")
            val deleted = nativeCache.asMap().keys
                .filterIsInstance<String>()
                .filter { it.startsWith(prefix) }
                .toList()
            deleted.forEach { springCache.evict(it) }
            deleted.size.toLong()
        }
    }

    override fun clearAll(): Long {
        val deleted = cacheManager.cacheNames.sumOf { cacheName ->
            val springCache = cacheManager.getCache(cacheName) ?: return@sumOf 0L
            val nativeCache = springCache.nativeCache as com.github.benmanes.caffeine.cache.Cache<*, *>
            val count = nativeCache.estimatedSize()
            springCache.clear()
            count
        }
        return deleted
    }

    override fun sizes(vararg caches: CacheNøkkelConfig): Map<String, Long> =
        caches.associate { cache ->
            val springCache = cacheManager.getCache(cache.name) ?: error("Cache $cache ikke funnet")
            val count = run {
                val nativeCache = springCache.nativeCache as com.github.benmanes.caffeine.cache.Cache<*, *>
                if (cache.extraPrefix == null) {
                    nativeCache.estimatedSize()
                } else {
                    val prefix = caffeineNøkkel(cache, "")
                    nativeCache.asMap().keys.count { it is String && it.startsWith(prefix) }.toLong()
                }
            }
            cache.fullName to count
        }
}