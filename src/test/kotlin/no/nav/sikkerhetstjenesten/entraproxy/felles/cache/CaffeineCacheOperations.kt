package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import com.github.benmanes.caffeine.cache.Cache
import org.springframework.cache.caffeine.CaffeineCacheManager
import java.time.Duration
import kotlin.reflect.KClass

class CaffeineCacheOperations(private val cacheManager: CaffeineCacheManager) : CacheOperations {

    override fun delete(cache: CacheNøkkelConfig, id: String): Long {
        val key = caffeineNøkkel(cache, id)
        val springCache = cacheManager.getCache(cache.name) ?: return 0L
        val existed = springCache.get(key) != null
        springCache.evict(key)
        return if (existed) 1L else 0L
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getOne(cache: CacheNøkkelConfig, id: String, clazz: KClass<T>): T? =
        cacheManager.getCache(cache.name)?.get(caffeineNøkkel(cache, id))?.get() as T?

    override fun putOne(cache: CacheNøkkelConfig, id: String, value: Any, ttl: Duration) {
        cacheManager.getCache(cache.name)?.put(caffeineNøkkel(cache, id), value)
    }

    private fun caffeineNøkkel(cache: CacheNøkkelConfig, id: String): String {
        val extra = cache.extraPrefix?.let { "$it:" } ?: ""
        return "$extra$id"
    }

    override fun clear(cache: CacheNøkkelConfig) {
        val springCache = cacheManager.getCache(cache.name) ?: return
        if (cache.extraPrefix == null) {
            springCache.clear()
        } else {
            val prefix = caffeineNøkkel(cache, "")
            val nativeCache = springCache.nativeCache
            if (nativeCache is Cache<*, *>) {
                nativeCache.asMap().keys
                    .filterIsInstance<String>()
                    .filter { it.startsWith(prefix) }
                    .forEach { springCache.evict(it) }
            }
        }
    }

    override fun sizes(vararg caches: CacheNøkkelConfig): Map<String, Long> =
        caches.associate { cache ->
            val springCache = cacheManager.getCache(cache.name) ?: error("Cache $cache ikke funnet")
            val count = run {
                val nativeCache = springCache.nativeCache
                if (nativeCache is Cache<*, *>) {
                    if (cache.extraPrefix == null) {
                        nativeCache.estimatedSize()
                    } else {
                        val prefix = caffeineNøkkel(cache, "")
                        nativeCache.asMap().keys.count { it is String && it.startsWith(prefix) }.toLong()
                    }
                } else {
                    error("Cache $cache ikke Caffeine")
                }
            }
            cache.fullName to count
        }
}
