package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import java.time.Duration
import kotlin.reflect.KClass

interface CacheOperations {
    fun delete(cache: CacheNøkkelConfig, id: String): Boolean
    fun <T : Any> getOne(cache: CacheNøkkelConfig, id: String, clazz: KClass<T>): T?
    fun putOne(cache: CacheNøkkelConfig, id: String, value: Any, ttl: Duration? = null)
    fun <T : Any> getMany(cache: CacheNøkkelConfig, ids: Set<String>, clazz: KClass<T>): Map<String, T?>
    fun putMany(cache: CacheNøkkelConfig, innslag: Map<String, Any>, ttl: Duration? = null)
    fun clear(cache: CacheNøkkelConfig): Long
    fun clear(caches: Set<CacheNøkkelConfig>) = caches.sumOf { clear(it) }
    fun clearAll(): Long
    fun size(cache: CacheNøkkelConfig): Long =
        sizes(cache).values.single()

    fun sizes(vararg caches: CacheNøkkelConfig): Map<String, Long>

}

inline fun <reified T : Any> CacheOperations.getOne(cfg: CacheNøkkelConfig, id: String): T? =
    getOne(cfg, id, T::class)

inline fun <reified T : Any> CacheOperations.getMany(cfg: CacheNøkkelConfig, ids: Set<String>): Map<String, T?> =
    getMany(cfg, ids, T::class)