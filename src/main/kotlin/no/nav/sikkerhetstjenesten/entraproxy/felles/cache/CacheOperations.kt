package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import java.time.Duration
import kotlin.reflect.KClass

interface CacheOperations {
    fun delete(cache: CachableConfig, id: String): Long
    fun <T : Any> getOne(cache: CachableConfig,id: String, clazz: KClass<T>): T?
    fun putOne(cache: CachableConfig, id: String, value: Any, ttl: Duration)
    fun <T : Any> getMany(cache: CachableConfig, ids: Set<String>, clazz: KClass<T>): Map<String, T?>
    fun putMany(cache: CachableConfig, innslag: Map<String, Any>, ttl: Duration)
    fun tilNøkkel(cache: CachableConfig, id: String): String
}