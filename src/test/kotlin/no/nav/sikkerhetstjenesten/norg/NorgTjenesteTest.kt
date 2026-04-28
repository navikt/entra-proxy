package no.nav.sikkerhetstjenesten.norg

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient
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
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.BASE_URI
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.ENHET_PATH
import org.springframework.web.util.UriComponentsBuilder.fromUriString


@RestClientTest(components = [NorgClientBeanConfig::class, NorgTjeneste::class, NorgConfig::class,NorgProxyClient::class, DefaultRestErrorHandler::class])
@ApplyExtension(SpringExtension::class)
class NorgTjenesteTest : BehaviorSpec() {

    @Autowired
    lateinit var tjeneste: NorgTjeneste
    @Autowired
    lateinit var server: MockRestServiceServer

    init {
        afterEach { server.verify() }

        Given("oppslag av navn for enhet") {
            When("enhet eksisterer") {
                Then("returnerer enhetens navn") {
                    server.expect(requestTo(ENHET_URI))
                        .andExpect(method(GET))
                        .andRespond(withSuccess("""
                           {
                             "enhetNr": 4242,
                             "navn": "NAV Testkontor"
                           } 
                        """.trimIndent(), APPLICATION_JSON))

                    tjeneste.navnFor(Enhetnummer("4242")) shouldBe "NAV Testkontor"
                }
            }
        }

/*
        Given("feilhaandtering") {
            When("tjenesten returnerer 404") {
                Then("kaster NotFoundRestException uten retry") {
                    server.expect(requestTo(ANSATT_URI))
                        .andExpect(method(GET))
                        .andRespond(withStatus(HttpStatus.NOT_FOUND))

                    shouldThrow<NotFoundRestException> {
                        tjeneste.enhet(ANSATTID)
                    }
                }
            }

            When("tjenesten returnerer 401") {
                Then("kaster IrrecoverableRestException uten retry") {
                    server.expect(requestTo(ANSATT_URI))
                        .andExpect(method(GET))
                        .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

                    shouldThrow<IrrecoverableRestException> {
                        tjeneste.enhet(ANSATTID)
                    }
                }
            }

            When("tjenesten returnerer 500") {
                Then("kaster RecoverableRestException etter 4 forsøk") {
                    server.expect(times(4), requestTo(ANSATT_URI))
                        .andExpect(method(GET))
                        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

                    shouldThrow<RecoverableRestException> {
                        tjeneste.enhet(ANSATTID)
                    }
                }
            }
        }

 */
    }

    companion object  {
        private val ENHET_URI = fromUriString("${BASE_URI}$ENHET_PATH")
            .buildAndExpand("4242").toUri()
    }
}