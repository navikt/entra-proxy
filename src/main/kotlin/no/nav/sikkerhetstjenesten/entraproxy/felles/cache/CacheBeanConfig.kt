package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.EVERYTHING
import io.lettuce.core.RedisClient
import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.PingableHealthIndicator
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter.nonLockingRedisCacheWriter
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
@Configuration(proxyBeanMethods = true)
@EnableCaching
@ConditionalOnGCP
class CacheBeanConfig(private val cf: RedisConnectionFactory, mapper: ObjectMapper,
                      private vararg val cfgs: CachableRestConfig) : CachingConfigurer {

    private val cacheMapper =
        mapper.copy().apply {
            activateDefaultTyping(polymorphicTypeValidator, EVERYTHING, PROPERTY)
        }

    @Bean
    override fun cacheManager()  =
        RedisCacheManager.builder(nonLockingRedisCacheWriter(cf))
            .withInitialCacheConfigurations(cfgs.associate { it.navn to cacheConfig(it) })
            .enableStatistics()
            .build()

    @Bean
    fun redisClient(cfg: CacheConfig) =
        RedisClient.create(cfg.cacheURI)

    @Bean
    fun cacheClient(client: RedisClient) =
        CacheClient(client)

    @Bean
    fun cacheHealthIndicator(pingable: CachePingable)  =
        PingableHealthIndicator(pingable)

    private fun cacheConfig(cfg: CachableRestConfig) =
        defaultCacheConfig()
            .entryTtl(cfg.varighet)
            .serializeKeysWith(fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(fromSerializer(GenericJackson2JsonRedisSerializer(cacheMapper)))
}