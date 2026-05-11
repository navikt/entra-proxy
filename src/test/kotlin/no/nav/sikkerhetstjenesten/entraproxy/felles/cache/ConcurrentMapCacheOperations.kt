package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Primary
import java.time.Duration
import kotlin.reflect.KClass

@Primary
class ConcurrentMapCacheOperations(private val mgr: CacheManager) : CacheOperations {

    override fun delete(cache: CachableConfig, id: String): Long {
        val c = mgr.getCache(cache.name) ?: return 0L
        val key = tilNøkkel(cache, id)
        val existed = c.get(key) != null
        c.evict(key)
        return if (existed) 1L else 0L
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getOne(cache: CachableConfig, id: String, clazz: KClass<T>): T? =
        mgr.getCache(cache.name)?.get(tilNøkkel(cache, id))?.get() as T?

    override fun putOne(cache: CachableConfig, id: String, value: Any, ttl: Duration) {
        mgr.getCache(cache.name)?.put(tilNøkkel(cache, id), value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getMany(cache: CachableConfig, ids: Set<String>, clazz: KClass<T>): Map<String, T?> =
        ids.associateWith { id ->
            mgr.getCache(cache.name)?.get(tilNøkkel(cache, id))?.get() as T?
        }.filterValues { it != null }

    override fun putMany(cache: CachableConfig, innslag: Map<String, Any>, ttl: Duration) {
        val c = mgr.getCache(cache.name) ?: return
        innslag.forEach { (id, value) -> c.put(tilNøkkel(cache, id), value) }
    }

    override fun tilNøkkel(cache: CachableConfig, id: String) =
        cache.extraPrefix ?.let { "$it:$id"  } ?: id
}