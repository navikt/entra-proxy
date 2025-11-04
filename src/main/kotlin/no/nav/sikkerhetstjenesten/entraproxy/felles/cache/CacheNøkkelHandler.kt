package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.boot.conditionals.EnvUtil.CONFIDENTIAL
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheClient.CachableConfig
import org.slf4j.LoggerFactory.getLogger
import org.springframework.data.redis.cache.RedisCacheConfiguration


class CacheNøkkelHandler(val configs: Map<String, RedisCacheConfiguration>, val mapper: ObjectMapper) {
    private val log = getLogger(javaClass)

    inline fun <reified T> fraJson(json: String): T =
        mapper.readValue(json)

    fun tilJson(value: Any): String =
        mapper.writeValueAsString(value)

    fun tilNøkkel(cache: CachableConfig, nøkkel: String): String {
        val prefix = prefixFor(cache)
        val extra = cache.extraPrefix?.let { "$it:" } ?: ""
        return "$prefix::$extra$nøkkel".also {
            log.trace(CONFIDENTIAL, "Lagt til prefix for {} -> {}", nøkkel, it)
        }
    }

    fun fraNøkkel(nøkkel: String): String {
        val elementer = CacheNøkkelElementer(nøkkel)
        log.trace(CONFIDENTIAL,"Fjernet prefix for {} -> {}",nøkkel, elementer.id)
        return elementer.id
    }

    private fun prefixFor(cache: CachableConfig): String =
        configs[cache.name]?.getKeyPrefixFor(cache.name)
            ?: throw IllegalStateException("Ingen cache med navn ${cache.name}")

}


