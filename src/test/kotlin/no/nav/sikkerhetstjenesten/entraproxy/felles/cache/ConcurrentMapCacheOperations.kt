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

    override fun tilNøkkel(cache: CachableConfig, id: String) =
        cache.extraPrefix ?.let { "$it:$id"  } ?: id
}