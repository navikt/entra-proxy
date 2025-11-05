package no.nav.sikkerhetstjenesten.entraproxy.felles.cache


import io.lettuce.core.RedisURI
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheConfig.Companion.VALKEY
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(VALKEY)
data class CacheConfig(val username: String, val password: String, val host: String, val port: String) {
    val cacheURI = RedisURI.Builder
        .redis(host, port.toInt())
        .withSsl(true)
        .withAuthentication(username, password)
        .build()

    companion object {
        const val VALKEY = "valkey"
    }
}
