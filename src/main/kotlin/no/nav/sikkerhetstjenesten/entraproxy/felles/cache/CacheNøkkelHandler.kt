package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheBeanConfig.Companion.VALKEY_MAPPER
import org.slf4j.LoggerFactory
import org.springframework.data.redis.cache.RedisCacheConfiguration
import tools.jackson.databind.json.JsonMapper

class CacheNøkkelHandler(val configs: Map<String, RedisCacheConfiguration?>,val mapper: JsonMapper = VALKEY_MAPPER) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun tilNøkkel(cache:  CachableConfig, nøkkel: String): String {
        val prefix = prefixFor(cache)
        val extra = cache.extraPrefix?.let { "$it:" } ?: ""
        return "$prefix$extra$nøkkel"
    }


    private fun prefixFor(cache: CachableConfig): String =
        configs[cache.name]?.getKeyPrefixFor(cache.name)
            ?: error("Ingen cache med navn ${cache.name}")

}