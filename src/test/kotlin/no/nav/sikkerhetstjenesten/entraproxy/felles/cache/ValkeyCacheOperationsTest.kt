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
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.ENHETER_GRAPH_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRUPPER_FOR_ANSATT_GRAPH_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.TEMA_GRAPH_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.UTVIDET_ANSATT_GRAPH_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.graph.UtvidetAnsatt
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
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
import java.util.UUID
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

        private val allTestCaches = setOf(
            GRUPPER_FOR_ANSATT_GRAPH_CACHE,
            UTVIDET_ANSATT_GRAPH_CACHE,
            ENHETER_GRAPH_CACHE,
            TEMA_GRAPH_CACHE,
            MEDLEMMER_CACHE,
            OID_CACHE,
            CacheNøkkelConfig(NORG),
        )

        override fun configureMessageConverters(builder: RedisMessageConverters.Builder) {
            builder.addCustomConverter(CacheNøkkelMessageConverter())
        }

        @Bean
        fun cacheManager() =
            builder(cf)
                .withInitialCacheConfigurations(
                    allTestCaches
                        .groupBy { it.name }
                        .mapValues { (_, caches) ->
                            defaultCacheConfig()
                                .prefixCacheNameWith(caches.first().name)
                                .disableCachingNullValues()
                        }
                )
                .build()

        @Bean
        fun valkeyCacheOperations(valkey: StringRedisTemplate) =
            ValkeyCacheOperations(
                valkey,
                object : CachableRestConfig {
                    override val navn = "entra-test"
                    override val caches = allTestCaches
                    override val varighet = ofSeconds(DEFAULT_TTL_SECONDS)
                }
            )

        @Bean
        fun cacheSizeAware(cache: CacheOperations) =
            CacheSizeAware(
                cache,
                object : CachableRestConfig {
                    override val navn = "entra-test"
                    override val caches = allTestCaches
                })
    }

    @MockkBean
    private lateinit var authContext: AuthContext

    @MockkBean(relaxed = true)
    private lateinit var oppfrisker: CacheOppfrisker

    private fun testValueFor(cacheConfig: CacheNøkkelConfig): Any = when (cacheConfig) {
        GRUPPER_FOR_ANSATT_GRAPH_CACHE -> G1
        UTVIDET_ANSATT_GRAPH_CACHE -> U1
        ENHETER_GRAPH_CACHE -> setOf(E1)
        TEMA_GRAPH_CACHE -> setOf(T1)
        MEDLEMMER_CACHE -> setOf(M1)
        OID_CACHE -> O1
        CacheNøkkelConfig(NORG) -> "Norg 1"
        else -> TEST_VALUE_1
    }

    private fun cacheEntriesFor(cacheConfig: CacheNøkkelConfig): Map<String, Any> = when (cacheConfig) {
        GRUPPER_FOR_ANSATT_GRAPH_CACHE -> mapOf(I1 to G1, I2 to G2)
        UTVIDET_ANSATT_GRAPH_CACHE -> mapOf(I1 to U1, I2 to U2)
        ENHETER_GRAPH_CACHE -> mapOf(I1 to setOf(E1), I2 to setOf(E2))
        TEMA_GRAPH_CACHE -> mapOf(I1 to setOf(T1), I2 to setOf(T2))
        MEDLEMMER_CACHE -> mapOf(I1 to setOf(M1), I2 to setOf(M2))
        OID_CACHE -> mapOf(I1 to O1, I2 to O2)
        CacheNøkkelConfig(NORG) -> mapOf(I1 to "Norg 1", I2 to "Norg 2")
        else -> mapOf(I1 to TEST_VALUE_1, I2 to TEST_VALUE_2)
    }

    private fun readMany(cacheConfig: CacheNøkkelConfig, ids: Set<String>) = when (cacheConfig) {
        GRUPPER_FOR_ANSATT_GRAPH_CACHE -> cache.getMany<EntraGruppe>(cacheConfig, ids)
        UTVIDET_ANSATT_GRAPH_CACHE -> cache.getMany<UtvidetAnsatt>(cacheConfig, ids)
        ENHETER_GRAPH_CACHE -> cache.getMany<Set<Enhet>>(cacheConfig, ids)
        TEMA_GRAPH_CACHE -> cache.getMany<Set<Tema>>(cacheConfig, ids)
        MEDLEMMER_CACHE -> cache.getMany<Set<Ansatt>>(cacheConfig, ids)
        OID_CACHE -> cache.getMany<UUID>(cacheConfig, ids)
        CacheNøkkelConfig(NORG) -> cache.getMany<String>(cacheConfig, ids)
        else -> cache.getMany<String>(cacheConfig, ids)
    }

    private fun assertReadOne(cacheConfig: CacheNøkkelConfig, key: String, expected: Any?) = when (cacheConfig) {
        GRUPPER_FOR_ANSATT_GRAPH_CACHE -> cache.getOne<EntraGruppe>(cacheConfig, key) shouldBe (expected as? EntraGruppe)
        UTVIDET_ANSATT_GRAPH_CACHE -> cache.getOne<UtvidetAnsatt>(cacheConfig, key) shouldBe (expected as? UtvidetAnsatt)
        ENHETER_GRAPH_CACHE -> cache.getOne<Set<Enhet>>(cacheConfig, key) shouldBe (expected as? Set<Enhet>)
        TEMA_GRAPH_CACHE -> cache.getOne<Set<Tema>>(cacheConfig, key) shouldBe (expected as? Set<Tema>)
        MEDLEMMER_CACHE -> cache.getOne<Set<Ansatt>>(cacheConfig, key) shouldBe (expected as? Set<Ansatt>)
        OID_CACHE -> cache.getOne<UUID>(cacheConfig, key) shouldBe (expected as? UUID)
        CacheNøkkelConfig(NORG) -> cache.getOne<String>(cacheConfig, key) shouldBe expected as? String
        else -> cache.getOne<String>(cacheConfig, key) shouldBe expected as? String
    }

    init {

        beforeEach {
            every { authContext.system } returns "test"
            every { authContext.clusterAndSystem } returns "test:dev-gcp"
            every { oppfrisker.cacheName } returns GRUPPER_FOR_ANSATT_GRAPH_CACHE.name
            every { oppfrisker.oppfrisk(any()) } returns Unit
            ALL_TEST_CACHES.forEach { cache.clear(it) }
        }

        Given("putMany og getMany") {
            ALL_TEST_CACHES.forEach { cacheConfig ->
                When("verdier legges i cache med kort TTL for ${cacheConfig.fullName}") {
                    Then("returneres ved oppslag og fjernes etter TTL") {
                        val values = cacheEntriesFor(cacheConfig)
                        cache.putMany(cacheConfig, values, ofSeconds(1))
                        val many = readMany(cacheConfig, IDS)
                        many.keys shouldBe IDS
                        many.values.toSet() shouldBe values.values.toSet()
                        eventually(TIMEOUT) {
                            readMany(cacheConfig, IDS).shouldBeEmpty()
                        }
                    }
                }
                When("kalles med tomt set for ${cacheConfig.fullName}") {
                    Then("returnerer tomt map") {
                        readMany(cacheConfig, emptySet()).shouldBeEmpty()
                    }
                }
                When("putMany kalles med tom map for ${cacheConfig.fullName}") {
                    Then("er kall et no-op uten sideeffekter") {
                        cache.putMany(cacheConfig, emptyMap(), ofSeconds(5))
                        cache.size(cacheConfig) shouldBe 0
                        readMany(cacheConfig, IDS).shouldBeEmpty()
                    }
                }
            }
        }

        Given("putOne uten eksplisitt TTL") {
            ALL_TEST_CACHES.forEach { cacheConfig ->
                When("verdien lagres til ${cacheConfig.fullName}") {
                    Then("settes TTL fra CachableRestConfig") {
                        val value = testValueFor(cacheConfig)
                        cache.putOne(cacheConfig, I1, value)

                        val ttl = valkey.getExpire(cacheConfig.tilNøkkel(I1))
                        (ttl in 1..DEFAULT_TTL_SECONDS) shouldBe true
                    }
                }
            }
        }

        Given("sletting av enkeltinnslag") {
            ALL_TEST_CACHES.forEach { cacheConfig ->
                When("nøkkelen eksisterer i ${cacheConfig.fullName}") {
                    Then("returnerer true og verdien er fjernet") {
                        val value = testValueFor(cacheConfig)
                        cache.putOne(cacheConfig, I1, value, ofSeconds(2))
                        assertSoftly {
                            assertReadOne(cacheConfig, I1, value)
                            cache.delete(cacheConfig, I1) shouldBe true
                            assertReadOne(cacheConfig, I1, null)
                        }
                    }
                }
                When("nøkkelen ikke eksisterer i ${cacheConfig.fullName}") {
                    Then("returnerer false") {
                        assertSoftly {
                            assertReadOne(cacheConfig, I1, null)
                            cache.delete(cacheConfig, I1) shouldBe false
                        }
                    }
                }
            }
        }

        Given("cacheSet") {
            When("ett sett med strenger legges i en Valkey-set") {
                Then("lagres som medlemmer i den angitte nøkkelen") {
                    val members = setOf("a", "b", "c")
                    cache.replaceSet("cache-set-test", members) shouldBe 3L
                    valkey.opsForSet().members("cache-set-test") shouldBe members
                }
            }
        }

        Given("cache-utløp") {
            ALL_TEST_CACHES.forEach { cacheConfig ->
                When("TTL løper ut for ${cacheConfig.fullName}") {
                    Then("Valkey publiserer expired-event som håndteres av ValkeyListener") {
                        every { oppfrisker.cacheName } returns cacheConfig.name
                        cache.putOne(cacheConfig, I1, TEST_VALUE_1, ofSeconds(1))

                        eventually(VALKEY_EVENT_TIMEOUTS) {
                            verify {
                                oppfrisker.oppfrisk(match {
                                    it.cacheName == cacheConfig.name && it.id == I1
                                })
                            }
                        }
                    }
                }

                When("nøkkel slettes i ${cacheConfig.fullName}") {
                    Then("Valkey publiserer del-event som håndteres av ValkeyListener") {
                        every { oppfrisker.cacheName } returns cacheConfig.name
                        cache.putOne(cacheConfig, I1, TEST_VALUE_1, ofSeconds(10))
                        cache.delete(cacheConfig, I1) shouldBe true

                        eventually(VALKEY_EVENT_TIMEOUTS) {
                            verify {
                                oppfrisker.oppfrisk(match {
                                    it.cacheName == cacheConfig.name && it.id == I1
                                })
                            }
                        }
                    }
                }
            }
        }

        Given("tømming av cache") {
            ALL_TEST_CACHES.forEach { cacheConfig ->
                When("cache inneholder verdier for ${cacheConfig.fullName}") {
                    Then("alle verdier i cachen fjernes") {
                        cache.putMany(cacheConfig, mapOf(I1 to TEST_VALUE_1, I2 to TEST_VALUE_2), ofSeconds(1))
                        cache.size(cacheConfig) shouldBe 2
                        cache.getMany<String>(cacheConfig, IDS).keys shouldBe IDS
                        cache.clear(cacheConfig)
                        cache.getMany<String>(cacheConfig, IDS).shouldBeEmpty()
                        cache.size(cacheConfig) shouldBe 0
                    }
                }
                When("cache er tom for ${cacheConfig.fullName}") {
                    Then("clear kaster ikke exception") {
                        cache.clear(cacheConfig)
                        cache.getMany<String>(cacheConfig, IDS).shouldBeEmpty()
                        cache.size(cacheConfig) shouldBe 0
                    }
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
            ALL_TEST_CACHES.forEach { cacheConfig ->
                When("getOne treffer cache for ${cacheConfig.fullName}") {
                    Then("registreres varighet med cache, operasjon og hit-resultat") {
                        cache.putOne(cacheConfig, I1, TEST_VALUE_1, ofSeconds(5))
                        cache.getOne<String>(cacheConfig, I1) shouldBe TEST_VALUE_1
                    }
                }

                When("getMany gir både treff og miss for ${cacheConfig.fullName}") {
                    Then("registreres varighet med delvis-resultat") {
                        cache.putOne(cacheConfig, I1, TEST_VALUE_1, ofSeconds(5))
                        cache.getMany<String>(cacheConfig, setOf(I1, I2)).keys shouldBe setOf(I1)
                    }
                }

                When("putOne feiler mot utilgjengelig Redis for ${cacheConfig.fullName}") {
                    Then("registreres varighet med feilet-resultat") {
                        redis.dockerClient.pauseContainerCmd(redis.containerId).exec()
                        try {
                            cache.putOne(cacheConfig, I1, TEST_VALUE_1, ofSeconds(30))
                        } finally {
                            redis.dockerClient.unpauseContainerCmd(redis.containerId).exec()
                        }
                    }
                }
            }
        }

        Given("antall innslag i cache") {
            ALL_TEST_CACHES.forEach { cacheConfig ->
                When("cache er tom for ${cacheConfig.fullName}") {
                    Then("returnerer 0") {
                        cache.size(cacheConfig) shouldBe 0
                    }
                }
                When("cache inneholder verdier for ${cacheConfig.fullName}") {
                    Then("returnerer antall innslag") {
                        cache.putMany(cacheConfig, mapOf(I1 to TEST_VALUE_1, I2 to TEST_VALUE_2), ofSeconds(5))
                        cache.size(cacheConfig) shouldBe 2
                    }
                }
                When("verdier fjernes fra ${cacheConfig.fullName}") {
                    Then("size oppdateres") {
                        cache.putMany(cacheConfig, mapOf(I1 to TEST_VALUE_1, I2 to TEST_VALUE_2), ofSeconds(5))
                        cache.size(cacheConfig) shouldBe 2
                        cache.delete(cacheConfig, I1)
                        cache.size(cacheConfig) shouldBe 1
                    }
                }
                When("clear kalles for ${cacheConfig.fullName}") {
                    Then("size blir 0") {
                        cache.putMany(cacheConfig, mapOf(I1 to TEST_VALUE_1, I2 to TEST_VALUE_2), ofSeconds(5))
                        cache.size(cacheConfig) shouldBe 2
                        cache.clear(cacheConfig)
                        cache.size(cacheConfig) shouldBe 0
                    }
                }
            }
        }

        Given("antall innslag via CacheSizeAware") {
            When("cache er tom") {
                Then("returnerer cache-størrelse fra valkey") {
                    cacheSizeAware.sizes() shouldBe ALL_TEST_CACHES.associate { it.fullName to 0L }
                }
            }
            When("cache inneholder verdier") {
                Then("returnerer antall innslag fra valkey") {
                    cache.putMany(GRUPPER_FOR_ANSATT_GRAPH_CACHE, mapOf(I1 to TEST_VALUE_1, I2 to TEST_VALUE_2), ofSeconds(5))
                    cacheSizeAware.sizes() shouldBe ALL_TEST_CACHES.associate { cache ->
                        if (cache == GRUPPER_FOR_ANSATT_GRAPH_CACHE) cache.fullName to 2L else cache.fullName to 0L
                    }
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
        private const val TEST_VALUE_1 = "test-value-1"
        private const val TEST_VALUE_2 = "test-value-2"
        private val G1 = EntraGruppe("Gruppe 1")
        private val G2 = EntraGruppe("Gruppe 2")
        private val T1 = Tema("ABC")
        private val T2 = Tema("DEF")
        private val E1 = Enhet(Enhet.Enhetnummer("1234"), "Enhet 1")
        private val E2 = Enhet(Enhet.Enhetnummer("5678"), "Enhet 2")
        private val U1 = UtvidetAnsatt(A1, "Visning 1", "Fornavn 1", "Etternavn 1", TIdent("ABC1234"), "e1@test", E1)
        private val U2 = UtvidetAnsatt(A2, "Visning 2", "Fornavn 2", "Etternavn 2", TIdent("DEF5678"), "e2@test", E2)
        private val M1 = Ansatt(A1, "Visning 1", "Fornavn 1", "Etternavn 1")
        private val M2 = Ansatt(A2, "Visning 2", "Fornavn 2", "Etternavn 2")
        private val O1 = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111")
        private val O2 = java.util.UUID.fromString("22222222-2222-2222-2222-222222222222")

        private val ALL_TEST_CACHES = setOf(
            GRUPPER_FOR_ANSATT_GRAPH_CACHE,
            UTVIDET_ANSATT_GRAPH_CACHE,
            ENHETER_GRAPH_CACHE,
            TEMA_GRAPH_CACHE,
            MEDLEMMER_CACHE,
            OID_CACHE,
            CacheNøkkelConfig(NORG),
        )

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
