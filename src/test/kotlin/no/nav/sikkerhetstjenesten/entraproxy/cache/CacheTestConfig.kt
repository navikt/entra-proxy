package no.nav.sikkerhetstjenesten.entraproxy.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean

@EnableCaching
abstract class CacheTestConfig(vararg cacheNames: String) {
    private val names = cacheNames

    @Bean
    fun cacheManager() = CaffeineCacheManager(*names).apply {
        setCaffeine(Caffeine.newBuilder().maximumSize(10_000))
    }

    @Bean
    fun cacheOperations(cacheManager: CacheManager) =
        CaffeineCacheOperations(cacheManager)
}