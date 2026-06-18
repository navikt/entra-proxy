package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import java.time.Duration
import kotlin.reflect.KClass

interface CacheOperations {
    fun size(cache: CacheNøkkelConfig): Long =
        sizes(cache).values.single()
    fun sizes(vararg caches: CacheNøkkelConfig): Map<String, Long>
    fun delete(cache: CacheNøkkelConfig, id: String): Long
    fun putOne(cache: CacheNøkkelConfig, id: String, value: Any, ttl: Duration)
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOne(cache: CacheNøkkelConfig, id: String, clazz: KClass<T>): T?
    fun clear(cache: CacheNøkkelConfig)
}