package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import org.slf4j.LoggerFactory
import org.springframework.data.redis.cache.RedisCacheConfiguration

class CacheNøkkelHandler(val configs: Map<String, RedisCacheConfiguration?>) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun tilNøkkel(cache: CachableConfig, nøkkel: String): String {
        val prefix = prefixFor(cache)
        val extra = cache.extraPrefix?.let { "$it:" } ?: ""
        return "$prefix$extra$nøkkel"
    }


    private fun prefixFor(cache: CachableConfig): String =
        configs[cache.name]?.getKeyPrefixFor(cache.name).also {
            log.debug("Prefix for cache ${cache.name} er: $it")
        }
            ?: error("Ingen cache med navn ${cache.name}")

}