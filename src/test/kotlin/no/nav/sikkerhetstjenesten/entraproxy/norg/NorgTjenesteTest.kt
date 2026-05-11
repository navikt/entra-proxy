package no.nav.sikkerhetstjenesten.entraproxy.norg

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ConcurrentMapCacheOperations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.http.HttpMethod.GET
import org.springframework.http.MediaType.APPLICATION_JSON
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.DefaultRestErrorHandler
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RecoverableRestException
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG_BASE_URI
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.ENHET_PATH
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjenesteTest.NorgTestConfig
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.test.web.client.ExpectedCount.times
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.util.UriComponentsBuilder.fromUriString


@RestClientTest
@ApplyExtension(SpringExtension::class)
@Import(value = [NorgClientBeanConfig::class, NorgTjeneste::class, NorgConfig::class, NorgPingable::class, DefaultRestErrorHandler::class, NorgTestConfig::class])
@EnableResilientMethods
class NorgTjenesteTest(@param:Autowired private val tjeneste: NorgTjeneste,
                       @param:Autowired private val server: MockRestServiceServer) : BehaviorSpec() {

    @Configuration
    @org.springframework.cache.annotation.EnableCaching
    class NorgTestConfig {
        @Bean fun cacheManager() =
            ConcurrentMapCacheManager(NORG)

        @Bean
        fun cacheOperations(cacheManager: CacheManager)  =
            ConcurrentMapCacheOperations(cacheManager)
    }

    @Autowired
    private lateinit var cacheManager: CacheManager
    @Autowired
    private lateinit var cache: CacheOperations

    init {
        beforeEach { cacheManager.getCache(NORG)?.clear() }
        afterEach { server.verify() }

        Given("oppslag av navn for enhet") {
            When("enhet eksisterer") {
                Then("returnerer enhetens navn") {
                    server.expect(requestTo(ENHET_URI))
                        .andExpect(method(GET))
                        .andRespond(withSuccess("""
                           {
                             "enhetNr": "$NUMMER",
                             "navn": "$NAVN"
                           } 
                        """.trimIndent(), APPLICATION_JSON))

                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN
                }
            }
        }
        Given("enhet finnes ikke") {
            When("tjenesten returnerer 404") {
                Then("kaster NotFoundRestException uten retry") {
                    server.expect(requestTo(ENHET_URI))
                        .andExpect(method(GET))
                        .andRespond(withStatus(NOT_FOUND))

                    shouldThrow<NotFoundRestException> {
                        tjeneste.navnFor(ENHETSNUMMER)
                    }
                }
            }
        }
        Given("feilhåndtering med retry") {
            When("tjenesten returnerer 500") {
                Then("prøver 4 ganger") {
                    server.expect(times(4),requestTo(ENHET_URI))
                        .andExpect(method(GET))
                        .andRespond(withStatus(INTERNAL_SERVER_ERROR))

                    shouldThrow<RecoverableRestException> {
                        tjeneste.navnFor(ENHETSNUMMER)
                    }
                }
            }
        }
        Given("caching av oppslag") {
            When("samme enhet hentes to ganger") {
                Then("kun ett HTTP-kall, andre kall hentes fra cache") {
                    server.expect(times(1), requestTo(ENHET_URI))
                        .andExpect(method(GET))
                        .andRespond(withSuccess("""
                           {
                             "enhetNr": "$NUMMER",
                             "navn": "$NAVN"
                           }
                        """.trimIndent(), APPLICATION_JSON))

                    cacheManager.getCache(NORG)?.get(CACHE_KEY) shouldBe null
                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN
                    cacheManager.getCache(NORG)?.get(CACHE_KEY)?.get() shouldBe NAVN
                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN
                }
            }
            When("cachen tømmes mellom kall") {
                Then("nytt HTTP-kall utføres") {
                    server.expect(times(2), requestTo(ENHET_URI))
                        .andExpect(method(GET))
                        .andRespond(withSuccess("""
                           {
                             "enhetNr": "$NUMMER",
                             "navn": "$NAVN"
                           }
                        """.trimIndent(), APPLICATION_JSON))

                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN
                    cacheManager.getCache(NORG)?.get(CACHE_KEY)?.get() shouldBe NAVN

                    cacheManager.getCache(NORG)?.clear()
                    cacheManager.getCache(NORG)?.get(CACHE_KEY) shouldBe null

                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN
                }
            }
        }
    }

    companion object  {
        private const val NAVN = "NAV Testkontor"
        private const val NUMMER = "4242"
        private val ENHETSNUMMER = Enhetnummer(NUMMER)
        private val ENHET_URI = fromUriString("${NORG_BASE_URI}$ENHET_PATH")
            .buildAndExpand(ENHETSNUMMER.verdi).toUri()
        private const val CACHE_KEY = "navnFor:$NUMMER"
    }
}