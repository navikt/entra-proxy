package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.PingableHealthIndicator
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter.nonLockingRedisCacheWriter
import org.springframework.data.redis.config.RedisListenerConfigurer
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisMessageConverters
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule.Builder

@Configuration(proxyBeanMethods = true)
@ConditionalOnGCP
class CacheBeanConfig(private val cf: RedisConnectionFactory,
                      private val errorHandler: CacheErrorHandler,
                      private vararg val cfgs: CachableRestConfig) : CachingConfigurer, RedisListenerConfigurer {

    private val log = getLogger(javaClass)



    override fun errorHandler() =
        errorHandler

    override fun configureMessageConverters(builder: RedisMessageConverters.Builder) {
        builder.addCustomConverter(CacheNøkkelMessageConverter())
    }


    @Bean
    override fun cacheManager() =
        RedisCacheManager.builder(nonLockingRedisCacheWriter(cf))
            .withInitialCacheConfigurations(cfgs.associate {
                it.navn to cacheConfig(it)
            }).enableStatistics()
            .build()


    @Bean
    fun cacheHealthIndicator(pingable: CachePingable) =
        PingableHealthIndicator(pingable)


    private fun cacheConfig(cfg: CachableRestConfig) =
        defaultCacheConfig()
            .entryTtl(cfg.varighet)
            .serializeKeysWith(fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(fromSerializer(
                ResilientValkeySerializer(GenericJacksonJsonRedisSerializer(VALKEY_MAPPER)))).apply {
                if (!cfg.cacheNulls) disableCachingNullValues()
            }

    @Bean(name = ["redisMessageListenerContainer"])
    fun redisMessageListenerContainer(cf: RedisConnectionFactory) =
        RedisMessageListenerContainer().apply {
            setConnectionFactory(cf)
            setRecoveryInterval(5_000) // retry subscription every 5s
            maxSubscriptionRegistrationWaitingTime = 30_000

            setErrorHandler {
                log.warn("Valkey listener feilet, retry om 5s", it)
            }
        }

    companion object {
        val VALKEY_MAPPER = JsonMapper.builder().polymorphicTypeValidator(NavPolymorphicTypeValidator()).apply {
            enable(INCLUDE_SOURCE_IN_LOCATION)
            addModules(Builder().build(),JacksonTypeInfoAddingValkeyModule())
        }.build()
    }
}


