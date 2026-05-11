package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheBeanConfig.Companion.VALKEY_MAPPER
import org.slf4j.LoggerFactory
import org.springframework.data.redis.cache.RedisCacheConfiguration
import tools.jackson.databind.json.JsonMapper
import kotlin.reflect.KClass

class CacheNøkkelHandler(val configs: Map<String, RedisCacheConfiguration?>,val mapper: JsonMapper = VALKEY_MAPPER) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun <T : Any> fraJson(json: String, clazz: KClass<T>): T =
        mapper.readValue(json, clazz.java)

    fun tilJson(value: Any): String =
        mapper.writeValueAsString(value)

    fun tilNøkkel(cache:  CachableConfig, nøkkel: String): String {
        val prefix = prefixFor(cache)
        val extra = cache.extraPrefix?.let { "$it:" } ?: ""
        return "$prefix$extra$nøkkel"
    }

    fun idFraNøkkel(nøkkel: String) = CacheNøkkel(nøkkel).id

    private fun prefixFor(cache: CachableConfig): String =
        configs[cache.name]?.getKeyPrefixFor(cache.name)
            ?: error("Ingen cache med navn ${cache.name}")

}