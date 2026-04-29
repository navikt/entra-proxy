package no.nav.sikkerhetstjenesten.norg

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgPingable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.http.HttpMethod.GET
import org.springframework.http.MediaType.APPLICATION_JSON
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgClientBeanConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.DefaultRestErrorHandler
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RecoverableRestException
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG_BASE_URI
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.ENHET_PATH
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.test.web.client.ExpectedCount.times
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.util.UriComponentsBuilder.fromUriString


@RestClientTest(components = [NorgClientBeanConfig::class,NorgTjeneste::class, NorgConfig::class,NorgPingable::class,NorgProxyClient::class, DefaultRestErrorHandler::class])
@ApplyExtension(SpringExtension::class)
@EnableResilientMethods
class NorgTjenesteTest(@param:Autowired private val tjeneste: NorgTjeneste,
                       @param:Autowired private val server: MockRestServiceServer) : BehaviorSpec() {


    init {
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
    }

    companion object  {
        private const val NAVN = "NAV Testkontor"
        private const val NUMMER = "4242"
        private val ENHETSNUMMER = Enhetnummer(NUMMER)
        private val ENHET_URI = fromUriString("${NORG_BASE_URI}$ENHET_PATH")
            .buildAndExpand(ENHETSNUMMER.verdi).toUri()
    }
}