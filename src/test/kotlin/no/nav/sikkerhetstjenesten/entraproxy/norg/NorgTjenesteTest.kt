package no.nav.sikkerhetstjenesten.entraproxy.norg

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheTestConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EnhetOperations
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod.GET
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter.create
import org.springframework.web.service.invoker.HttpServiceProxyFactory.builderFor
import org.springframework.web.service.invoker.createClient

@RestClientTest
class NorgTjenesteTest(private val server: MockRestServiceServer, private val tjeneste: EnhetOperations) : BehaviorSpec() {
    init {
        beforeEach {
            server.reset()
        }

        afterEach {
            server.verify()
        }

        Given("et enhetsnavn fra Norg") {
            When("navnet for samme enhet slås opp to ganger") {
                Then("returneres navnet og Norg kalles bare én gang") {
                    server.expect(requestTo("http://localhost/norg2/api/v1/enhet/0315"))
                        .andExpect(method(GET))
                        .andRespond(withSuccess("""{"enhetNr":315,"navn":"NAV Testenhet"}""", APPLICATION_JSON))

                    repeat(2) {
                        tjeneste.navn(Enhetnummer("0315")) shouldBe "NAV Testenhet"
                    }
                }
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    class NorgTestConfig : CacheTestConfig(NORG) {
        @Bean
        fun norgProxyClient(builder: RestClient.Builder): NorgProxyClient =
            builderFor(create(builder.baseUrl("http://localhost").build())).build().createClient()

        @Bean
        fun norgClientAdapter(client: NorgProxyClient) = NorgClientAdapter(client)

        @Bean
        fun norgTjeneste(adapter: NorgClientAdapter) = NorgTjeneste(adapter)
    }
}
