package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS
import com.fasterxml.jackson.annotation.JsonTypeInfo.Value.construct
import io.lettuce.core.RedisClient
import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheClient.Companion.CACHE_SIZE_SCRIPT
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
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.stereotype.Component
import tools.jackson.core.Version.unknownVersion
import tools.jackson.databind.AnnotationIntrospector
import tools.jackson.databind.DatabindContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.PolymorphicTypeValidator
import tools.jackson.databind.jsontype.PolymorphicTypeValidator.Validity.ALLOWED
import tools.jackson.databind.jsontype.PolymorphicTypeValidator.Validity.DENIED
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.KotlinModule.Builder

@Configuration(proxyBeanMethods = true)
@EnableCaching
@ConditionalOnGCP
class CacheBeanConfig(private val cf: RedisConnectionFactory,
                      private vararg val cfgs: CachableRestConfig) : CachingConfigurer {


    private val mapper = JsonMapper.builder().polymorphicTypeValidator(NavPolymorphicTypeValidator()).apply {
        addModule(Builder().build())
        addModule(JacksonTypeInfoAddingValkeyModule())
    }.build()

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any?> =
        RedisTemplate<String, Any?>().apply {
            connectionFactory = cf
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
    fun cacheHealthIndicator(pingable: CachePingable)  =
        PingableHealthIndicator(pingable)

    private fun cacheConfig(cfg: CachableRestConfig) =
        defaultCacheConfig()
            .entryTtl(cfg.varighet)
            .serializeKeysWith(fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(fromSerializer(GenericJacksonJsonRedisSerializer(mapper)))
}

class NavPolymorphicTypeValidator(private vararg val allowedPrefixes: String = arrayOf("no.nav.","java.", "kotlin.")) : PolymorphicTypeValidator() {

    override fun validateBaseType(ctx: DatabindContext, base: JavaType) = validityFor(base.rawClass.name)

    override fun validateSubClassName(ctx: DatabindContext, base: JavaType, subClassName: String)  = validityFor(subClassName)

    override fun validateSubType(ctx: DatabindContext, base: JavaType, subType: JavaType) = validityFor(subType.rawClass.name)

    private fun validityFor(className: String) =
        if (allowedPrefixes.any { className.startsWith(it) }) ALLOWED else DENIED
}

/**
Dette er en modul for Jackson 3 Json-serialisering som legger til en egendefinert AnnotationIntrospector. Denne introspektoren styrer hvordan typeinformasjon håndteres ved serialisering, slik at objekter får med seg typeinformasjon i JSON-feltet @class.
 */
class JacksonTypeInfoAddingValkeyModule : SimpleModule() {
    override fun setupModule(ctx: SetupContext) {
        ctx.insertAnnotationIntrospector(object : AnnotationIntrospector() {
            override fun findTypeResolverBuilder(config: MapperConfig<*>, ann: Annotated) =
                StdTypeResolverBuilder().init(
                    construct(CLASS, PROPERTY, "@class", null, true, true), null)
            override fun version() = unknownVersion()
        })
    }
}

@Component
class CacheKeyCounter(private val redisTemplate: RedisTemplate<String, Any>) {
    val script = DefaultRedisScript<Long>(CACHE_SIZE_SCRIPT)

    fun count(prefix: String): Long {
        val keys = listOf<String>() // No keys, only ARGV
        val args = listOf(prefix)
        val result: Long? = redisTemplate.execute(script, keys, *args.toTypedArray())
        return result ?: 0L

    }
}