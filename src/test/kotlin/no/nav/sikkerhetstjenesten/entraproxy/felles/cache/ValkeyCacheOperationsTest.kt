package no.nav.sikkerhetstjenesten.entraproxy.felles.cache


import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheOperationsTest.ValkeyCacheTestConfig
import com.ninjasquad.springmockk.MockkBean
import com.redis.testcontainers.RedisContainer
import com.redis.testcontainers.RedisContainer.DEFAULT_IMAGE_NAME
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AuthContext
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.CacheSizeAware
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.isProd
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRUPPER_FOR_ANSATT_GRAPH_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppe
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig
import org.springframework.data.redis.cache.RedisCacheManager.builder
import org.springframework.data.redis.config.RedisListenerConfigurer
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.RedisMessageConverters
import org.springframework.test.context.ContextConfiguration
import java.time.Duration.ofSeconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
@DataRedisTest
@ContextConfiguration(classes = [ValkeyCacheTestConfig::class,ValkeyEventListeningCacheOppfrisker::class])
@EnableAutoConfiguration
class ValkeyCacheOperationsTest(
    private val cache: CacheOperations,
    private val cacheSizeAware: CacheSizeAware,
    private val valkey: StringRedisTemplate) : BehaviorSpec() {

    @TestConfiguration
    class ValkeyCacheTestConfig(private val cf: RedisConnectionFactory) : RedisListenerConfigurer{

        override fun configureMessageConverters(builder: RedisMessageConverters.Builder) {
            builder.addCustomConverter(CacheNøkkelMessageConverter())
        }

        @Bean
        fun cacheManager() =
            builder(cf)
                .withInitialCacheConfigurations(
                    mapOf(GRUPPER_FOR_ANSATT_GRAPH_CACHE.name to defaultCacheConfig()
                        .prefixCacheNameWith(GRUPPER_FOR_ANSATT_GRAPH_CACHE.name)
                        .disableCachingNullValues()))
                .build()

        @Bean
        fun valkeyCacheOperations(valkey: StringRedisTemplate) =
            ValkeyCacheOperations(
                valkey,
                object : CachableRestConfig {
                    override val navn = "entra-test"
                    override val caches = setOf(GRUPPER_FOR_ANSATT_GRAPH_CACHE)
                    override val varighet = ofSeconds(DEFAULT_TTL_SECONDS)
                }
            )

        @Bean
        fun cacheSizeAware(cache: CacheOperations) =
            CacheSizeAware(
                cache,
                object : CachableRestConfig {
                    override val navn = "entra-test"
                    override val caches = setOf(GRUPPER_FOR_ANSATT_GRAPH_CACHE)
                })
    }

    @MockkBean
    private lateinit var authContext: AuthContext

    @MockkBean(relaxed = true)
    private lateinit var oppfrisker: CacheOppfrisker


    init {

        beforeEach {
            every { authContext.system } returns "test"
            every { authContext.clusterAndSystem } returns "test:dev-gcp"
            every { oppfrisker.cacheName } returns GRUPPER_FOR_ANSATT_GRAPH_CACHE.name
            every { oppfrisker.oppfrisk(any()) } returns Unit
            cache.clear(GRUPPER_FOR_ANSATT_GRAPH_CACHE)
        }

        Given("putMany og getMany") {
            When("verdier legges i cache med kort TTL") {
                Then("returneres ved oppslag og fjernes etter TTL") {
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to G1, I2 to G2), ofSeconds(1))
                    val many = cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, IDS)
                    many.keys shouldBe IDS
                    many.values shouldBe listOf(G1, G2)
                    eventually(TIMEOUT) {
                        cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, IDS).shouldBeEmpty()
                    }
                }
            }
            When("kalles med tomt set") {
                Then("returnerer tomt map") {
                    cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, emptySet()).shouldBeEmpty()
                }
            }
            When("putMany kalles med tom map") {
                Then("er kall et no-op uten sideeffekter") {
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, emptyMap(), ofSeconds(5))
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 0
                    cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, IDS).shouldBeEmpty()
                }
            }
        }

        Given("putOne uten eksplisitt TTL") {
            When("verdien lagres") {
                Then("settes TTL fra CachableRestConfig") {
                    cache.putOne(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1, A1)

                    val ttl = valkey.getExpire(GRUPPER_FOR_ANSATT_GRAPH_CACHE.tilNøkkel(I1))
                    (ttl in 1..DEFAULT_TTL_SECONDS) shouldBe true
                }
            }
        }

        Given("sletting av enkeltinnslag") {
            When("nøkkelen eksisterer") {
                Then("returnerer true og verdien er fjernet") {
                    cache.putOne(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1, G1, ofSeconds(2))
                    assertSoftly {
                        cache.getOne<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1) shouldBe G1
                        cache.delete(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1) shouldBe true
                        cache.getOne<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1).shouldBeNull()
                    }
                }
            }
            When("nøkkelen ikke eksisterer") {
                Then("returnerer false") {
                    assertSoftly {
                        cache.getOne<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1).shouldBeNull()
                        cache.delete(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1) shouldBe false
                    }
                }
            }
        }

        Given("cache-utløp") {
            When("TTL løper ut") {
                Then("Valkey publiserer expired-event som håndteres av ValkeyListener") {


                    cache.putOne(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1, A1, ofSeconds(1))

                    eventually(VALKEY_EVENT_TIMEOUTS) {
                        verify {
                            oppfrisker.oppfrisk(match {
                                it.cacheName == GRUPPER_FOR_ANSATT_GRAPH_CACHE.name && it.id == I1
                            })
                        }
                    }
                }
            }

            When("nøkkel slettes") {
                Then("Valkey publiserer del-event som håndteres av ValkeyListener") {
                    cache.putOne(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1, A1, ofSeconds(10))
                    cache.delete(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1) shouldBe true

                    eventually(VALKEY_EVENT_TIMEOUTS) {
                        verify {
                            oppfrisker.oppfrisk(match {
                                it.cacheName == GRUPPER_FOR_ANSATT_GRAPH_CACHE.name && it.id == I1
                            })
                        }
                    }
                }
            }

        }

        Given("tømming av cache") {
            When("cache inneholder verdier") {
                Then("alle verdier i cachen fjernes") {
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to G1, I2 to G2), ofSeconds(1))
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 2
                    cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, IDS).keys shouldBe IDS
                    cache.clear(GRUPPER_FOR_ANSATT_GRAPH_CACHE)
                    cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, IDS).shouldBeEmpty()
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 0
                }
            }
            When("cache er tom") {
                Then("clear kaster ikke exception") {
                    cache.clear(GRUPPER_FOR_ANSATT_GRAPH_CACHE)
                    cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, IDS).shouldBeEmpty()
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 0
                }
            }
        }

        Given("clear i prod-miljø") {
            beforeEach {
                mockkObject(ClusterUtils.Companion)
                every { isProd } returns true
            }
            afterEach { unmockkObject(ClusterUtils.Companion) }

            When("clear kalles") {
                Then("kaster IllegalStateException fordi clear er blokkert i prod") {
                    shouldThrow<IllegalStateException> { cache.clear(GRUPPER_FOR_ANSATT_GRAPH_CACHE) }
                        .message shouldContain "prod"
                }
            }
        }

        Given("graceful degradation ved Redis-feil") {
            When("getOne kalles mot utilgjengelig Redis") {
                Then("returnerer null i stedet for å kaste exception") {
                    cache.putOne(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1, G1, ofSeconds(30))
                    cache.getOne<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1) shouldBe G1
                    redis.dockerClient.pauseContainerCmd(redis.containerId).exec()
                    try {
                        cache.getOne<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1).shouldBeNull()
                    } finally {
                        redis.dockerClient.unpauseContainerCmd(redis.containerId).exec()
                    }
                }
            }
            When("getMany kalles mot utilgjengelig Redis") {
                Then("returnerer tomt map i stedet for å kaste exception") {
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to A1, I2 to A2), ofSeconds(30))
                    redis.dockerClient.pauseContainerCmd(redis.containerId).exec()
                    try {
                        cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, IDS).shouldBeEmpty()
                    } finally {
                        redis.dockerClient.unpauseContainerCmd(redis.containerId).exec()
                    }
                }
            }
            When("putOne kalles mot utilgjengelig Redis") {
                Then("kaster ikke exception") {
                    redis.dockerClient.pauseContainerCmd(redis.containerId).exec()
                    try {
                        cache.putOne(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1, A1, ofSeconds(30))
                    } finally {
                        redis.dockerClient.unpauseContainerCmd(redis.containerId).exec()
                    }
                }
            }
            When("putMany kalles mot utilgjengelig Redis") {
                Then("kaster ikke exception") {
                    redis.dockerClient.pauseContainerCmd(redis.containerId).exec()
                    try {
                        cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to A1, I2 to A2), ofSeconds(30))
                    } finally {
                        redis.dockerClient.unpauseContainerCmd(redis.containerId).exec()
                    }
                }
            }
            When("putMany feiler først mot utilgjengelig Redis og Redis blir tilgjengelig igjen") {
                Then("tilkoblingen er fortsatt brukbar og neste putMany lagrer riktig") {
                    redis.dockerClient.pauseContainerCmd(redis.containerId).exec()
                    try {
                        cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to G1), ofSeconds(30))
                    } finally {
                        redis.dockerClient.unpauseContainerCmd(redis.containerId).exec()
                    }
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I2 to G2), ofSeconds(30))
                    eventually(TIMEOUT) {
                        cache.getOne<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I2) shouldBe G2
                    }
                }
            }
        }

        Given("cache-metrikker") {
            When("getOne treffer cache") {
                Then("registreres varighet med cache, operasjon og hit-resultat") {
                    cache.putOne(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1, G1, ofSeconds(5))

                    cache.getOne<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1) shouldBe G1

                }
            }

            When("getMany gir både treff og miss") {
                Then("registreres varighet med delvis-resultat") {
                    cache.putOne(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1, G1, ofSeconds(5))

                    cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, setOf(I1, I2)).keys shouldBe setOf(I1)
                }
            }

            When("putOne feiler mot utilgjengelig Redis") {
                Then("registreres varighet med feilet-resultat") {
                    redis.dockerClient.pauseContainerCmd(redis.containerId).exec()
                    try {
                        cache.putOne(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1, G1, ofSeconds(30))
                    } finally {
                        redis.dockerClient.unpauseContainerCmd(redis.containerId).exec()
                    }
                }
            }
        }

        Given("antall innslag i cache") {
            When("cache er tom") {
                Then("returnerer 0") {
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 0
                }
            }
            When("cache inneholder verdier") {
                Then("returnerer antall innslag") {
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to A1, I2 to A2), ofSeconds(5))
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 2
                }
            }
            When("verdier fjernes") {
                Then("size oppdateres") {
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to A1, I2 to A2), ofSeconds(5))
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 2
                    cache.delete(GRUPPER_FOR_ANSATT_GRAPH_CACHE, I1)
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 1
                }
            }
            When("clear kalles") {
                Then("size blir 0") {
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to A1, I2 to A2), ofSeconds(5))
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 2
                    cache.clear(GRUPPER_FOR_ANSATT_GRAPH_CACHE)
                    cache.size(GRUPPER_FOR_ANSATT_GRAPH_CACHE) shouldBe 0
                }
            }
        }

        Given("antall innslag via CacheSizeAware") {
            When("cache er tom") {
                Then("returnerer cache-størrelse fra valkey") {
                    cacheSizeAware.sizes() shouldBe mapOf(GRUPPER_FOR_ANSATT_GRAPH_CACHE.fullName to 0L)
                }
            }
            When("cache inneholder verdier") {
                Then("returnerer antall innslag fra valkey") {
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to A1, I2 to A2), ofSeconds(5))
                    cacheSizeAware.sizes() shouldBe mapOf(GRUPPER_FOR_ANSATT_GRAPH_CACHE.fullName to 2L)
                }
            }
        }

        Given("serialisering av EntraGruppe") {
            When("EntraGruppe lagres og hentes via putMany/getMany") {
                Then("korrekt EntraGruppe returneres for hver nøkkel") {
                    val entries = mapOf(I1 to G1, I2 to G2)
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, entries, ofSeconds(5))

                    assertSoftly(cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, IDS)) {
                        this[I1] shouldBe G1
                        this[I2] shouldBe G2
                    }
                }
            }
            When("EntraGruppe lagres via putMany uten TTL") {
                Then("korrekt EntraGruppe returneres for hver nøkkel") {
                    val entries = mapOf(I1 to G1, I2 to G2)
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, entries)

                    assertSoftly(cache.getMany<EntraGruppe>(GRUPPER_FOR_ANSATT_GRAPH_CACHE, IDS)) {
                        this[I1] shouldBe G1
                        this[I2] shouldBe G2
                    }
                }
            }
        }
    }

    private companion object {
        @ServiceConnection
        private val redis = RedisContainer(DEFAULT_IMAGE_NAME)
        private const val DEFAULT_TTL_SECONDS = 12L
        private val A1 = AnsattId("E123456")
        private val I1 = A1.verdi
        private val A2 = AnsattId("E654321")
        private val I2 = A2.verdi
        private val G1 = EntraGruppe( "Gruppe 1")
        private val G2 = EntraGruppe( "Gruppe 2")

                private val IDS = setOf(I1, I2)
        private val TIMEOUT = eventuallyConfig {
            duration = 2.seconds
            interval = 100.milliseconds
        }
        private val VALKEY_EVENT_TIMEOUTS = eventuallyConfig {
            duration = 5.seconds
            interval = 100.milliseconds
        }
    }
}
