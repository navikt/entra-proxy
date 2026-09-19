package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@EnableCaching
@TestConfiguration
abstract class CacheTestConfig(vararg cacheNames: String) {
    private val names = cacheNames

    @Bean
    fun cacheManager() = CaffeineCacheManager(*names).apply {
        setCaffeine(Caffeine.newBuilder().maximumSize(10_000))
    }

    @Primary
    @Bean
    fun cacheOperations(cacheManager: CacheManager) =
        CaffeineCacheOperations(cacheManager)
}