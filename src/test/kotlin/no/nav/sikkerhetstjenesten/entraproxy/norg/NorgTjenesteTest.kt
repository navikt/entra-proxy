package no.nav.sikkerhetstjenesten.entraproxy.norg

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import com.ninjasquad.springmockk.MockkBean
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.getOne
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.DefaultRestErrorHandler
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RecoverableRestException
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG_CACHE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.ContextConfiguration
import java.net.URI

@ContextConfiguration(classes = [NorgClientBeanConfig::class, NorgTjeneste::class, NorgConfig::class, NorgTestConfig::class, DefaultRestErrorHandler::class])
@TestPropertySource(properties = ["spring.http.serviceclient.norg.base-url=http://norg2.org", "norg.varighet=10s"])
@ApplyExtension(SpringExtension::class)
class NorgTjenesteTest : BehaviorSpec() {

    @Autowired private lateinit var tjeneste: NorgTjeneste
    @Autowired private lateinit var cache: CacheOperations
    @MockkBean private lateinit var client: NorgProxyClient

    init {
        beforeEach {
            cache.clear(NORG_CACHE)
            clearMocks(client)
        }

        Given("oppslag av navn for enhet") {
            When("enhet eksisterer") {
                Then("returnerer enhetens navn") {
                    every { client.enhetFor(NUMMER) } returns NorgEnhetRespons(NUMMER.toInt(), NAVN)

                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN
                }
            }
        }

        Given("enhet finnes ikke") {
            When("tjenesten returnerer 404") {
                Then("kaster NotFoundRestException uten retry") {
                    every { client.enhetFor(NUMMER) } throws
                        NotFoundRestException(URI.create("http://www.vg.no"), "not found")

                    shouldThrow<NotFoundRestException> {
                        tjeneste.navnFor(ENHETSNUMMER)
                    }
                }
            }
        }

        Given("feilhåndtering med retry") {
            When("tjenesten returnerer 500") {
                Then("prøver 4 ganger") {
                    every {
                        client.enhetFor(NUMMER)
                    } throws
                        RecoverableRestException(INTERNAL_SERVER_ERROR, URI.create("http://www.vg.no"), "server error")

                    shouldThrow<RecoverableRestException> {
                        tjeneste.navnFor(ENHETSNUMMER)
                    }

                    verify(exactly = 4) { client.enhetFor(NUMMER) }
                }
            }
        }

        Given("caching av oppslag") {
            When("samme enhet hentes to ganger") {
                Then("kun ett kall til klienten, andre kall hentes fra cache") {
                    every { client.enhetFor(NUMMER) } returns NorgEnhetRespons(NUMMER.toInt(), NAVN)
                    cache.getOne<String>(NORG_CACHE, NUMMER) shouldBe null
                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN
                    cache.getOne<String>(NORG_CACHE, NUMMER) shouldBe NAVN
                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN
                    verify(exactly = 1) { client.enhetFor(NUMMER) }
                }
            }

            When("cachen tømmes mellom kall") {
                Then("nytt kall til klienten utføres") {
                    every { client.enhetFor(NUMMER) } returns NorgEnhetRespons(NUMMER.toInt(), NAVN)

                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN
                    cache.getOne<String>(NORG_CACHE, NUMMER) shouldBe NAVN

                    cache.clear(NORG_CACHE)
                    cache.getOne<String>(NORG_CACHE, NUMMER) shouldBe null

                    tjeneste.navnFor(ENHETSNUMMER) shouldBe NAVN

                    verify(exactly = 2) { client.enhetFor(NUMMER) }
                }
            }
        }
    }

    companion object {
        private const val NAVN = "NAV Testkontor"
        private const val NUMMER = "4242"
        private val ENHETSNUMMER = Enhetnummer(NUMMER)
    }
}
