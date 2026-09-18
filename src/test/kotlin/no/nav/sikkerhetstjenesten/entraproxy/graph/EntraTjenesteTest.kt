package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheTestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CaffeineCacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.ENTRA_OID
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod.GET
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter.create
import org.springframework.web.service.invoker.HttpServiceProxyFactory.builderFor
import org.springframework.web.service.invoker.createClient
import java.util.UUID.randomUUID

@RestClientTest
class EntraTjenesteTest(
    private val server: MockRestServiceServer,
    private val entra: EntraTjeneste,
    private val oid: EntraOidTjeneste) : BehaviorSpec() {

    @MockkBean(relaxed = true)
    private lateinit var norg: NorgTjeneste

    init {

        beforeEach {
            server.reset()
        }
        afterEach {
            server.verify()
        }

        Given("medlemmer-endepunkt") {
            When("det finnes medlemmer") {
                Then("skal responsen inneholde forventede medlemmer, og andre kall skal treffe cachen") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/groups/$GROUP_ID/members")
                    }.andRespond(withSuccess(gruppeMedlemmerContract, APPLICATION_JSON))

                    repeat(2) {
                        entra.medlemmerIGruppe("En gruppe", GROUP_ID) shouldBe setOf(ANSATT)
                    }
                }
            }
        }

        Given("medlemmer-endepunkt med paginering") {
            When("Graph svarer med flere sider via @odata.nextLink") {
                Then("skal alle sidene hentes og medlemmene fra alle sidene returneres") {
                    val gruppeId = randomUUID()
                    val nesteSideUri = "$baseUrl/groups/$gruppeId/members?\$skiptoken=abc"
                    val medlem2Id = "${randomUUID()}"

                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/groups/$gruppeId/members")
                    }.andRespond(withSuccess(
                        """
                        {
                          "@odata.nextLink": "$nesteSideUri",
                          "value": [
                            { "id": "$medlemId", "displayName": "Ola Nordmann", "givenName": "Ola", "surname": "Nordmann", "onPremisesSamAccountName": "E123456" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))

                    server.expect { request ->
                        request.method == GET && request.uri.toString() == nesteSideUri
                    }.andRespond(withSuccess(
                        """
                        {
                          "value": [
                            { "id": "$medlem2Id", "displayName": "Kari Nordmann", "givenName": "Kari", "surname": "Nordmann", "onPremisesSamAccountName": "E654321" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))

                    entra.gruppeMedlemmer("$gruppeId") shouldBe setOf(
                        ANSATT,
                        Ansatt(AnsattId("E654321"), "Kari Nordmann", "Kari", "Nordmann")
                    )
                }
            }
        }

        Given("medlemmer-endepunkt med et medlem uten onPremisesSamAccountName") {
            When("gruppen inneholder en nøstet gruppe eller tjenestekonto") {
                Then("skal medlemmet ignoreres uten at kallet feiler") {
                    val gruppeId = randomUUID()
                    val nøstetGruppeId = "${randomUUID()}"

                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/groups/$gruppeId/members")
                    }.andRespond(withSuccess(
                        """
                        {
                          "value": [
                            { "id": "$medlemId", "displayName": "Ola Nordmann", "givenName": "Ola", "surname": "Nordmann", "onPremisesSamAccountName": "E123456" },
                            { "id": "$nøstetGruppeId", "displayName": "En nøstet gruppe" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))

                    entra.gruppeMedlemmer("$gruppeId") shouldBe setOf(ANSATT)
                }
            }
        }

        Given("users-endepunkt for oppslag av oid") {
            When("det finnes nøyaktig én bruker for navident") {
                Then("skal oid returneres") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users")
                    }.andRespond(withSuccess(oidContract, APPLICATION_JSON))
                    repeat(2) {
                        oid.ansattOid(AnsattId("A123456")) shouldBe OID
                    }
                }
            }
        }

        Given("groups-endepunkt for oppslag av gruppe-oid") {
            When("det finnes en gruppe med gitt navn") {
                Then("skal gruppens oid returneres") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/groups")
                    }.andRespond(withSuccess(gruppeContract, APPLICATION_JSON))

                    repeat(2) {
                        oid.gruppeOid("En gruppe") shouldBe GRUPPE_OID
                    }
                }
            }
        }

        Given("memberOf-endepunkt for temaer") {
            When("ansatt er medlem av en tema-gruppe") {
                Then("skal temaet returneres") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users/$ANSATT_OID/memberOf")
                    }.andRespond(withSuccess(temaContract, APPLICATION_JSON))

                    repeat(2) {
                        entra.tema(AnsattId("A123456"), ANSATT_OID) shouldBe setOf(Tema("AAP"))
                    }
                }
            }
        }

        Given("memberOf-endepunkt for enheter") {
            When("ansatt er medlem av en enhet-gruppe") {
                Then("skal enhetsnummeret returneres") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users/$ANSATT_OID/memberOf")
                    }.andRespond(withSuccess(enhetContract, APPLICATION_JSON))

                    repeat(2) {
                        entra.enheter(AnsattId("A123456"), ANSATT_OID).map { it.enhetnummer.verdi } shouldBe listOf("1234")
                    }
                }
            }
        }

        Given("memberOf-endepunkt for grupper") {
            When("ansatt er medlem av en gruppe") {
                Then("skal gruppen returneres") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users/$ANSATT_OID/memberOf")
                    }.andRespond(withSuccess(grupperContract, APPLICATION_JSON))

                    repeat(2) {
                        entra.grupperForAnsatt(AnsattId("A123456"), ANSATT_OID) shouldBe setOf(EntraGruppe("0000-GA-MIN_ROLLE"))
                    }
                }
            }
        }

        Given("users-endepunkt for utvidet ansatt") {
            When("det finnes en bruker for navident") {
                Then("skal utvidet ansatt-informasjon returneres") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users")
                    }.andRespond(withSuccess(utvidetAnsattContract, APPLICATION_JSON))

                    repeat(2) {
                        val respons = entra.utvidetAnsatt(AnsattId("A123456"))
                        respons?.navIdent shouldBe AnsattId("A123456")
                        respons?.fornavn shouldBe "Kari"
                        respons?.etternavn shouldBe "Nordmann"
                        respons?.enhet?.enhetnummer?.verdi shouldBe "1234"
                    }
                }
            }
        }
    }

    @TestConfiguration
    class EntraTestConfig : CacheTestConfig(MEDLEMMER,GRAPH,ENTRA_OID)

    @TestConfiguration
    class TestBeanConfig {

        @Bean
        fun entraGraphClient(builder: RestClient.Builder): EntraGraphClient =
            builderFor(create(builder.baseUrl(baseUrl).build())).build().createClient()

        @Bean
        fun entraOidTjeneste(client: EntraGraphClient) = EntraOidTjeneste(client)

        @Bean
        fun entraTjeneste(client: EntraGraphClient, norg: NorgTjeneste, oid: EntraOidTjeneste, cache: CaffeineCacheOperations) =
            EntraTjeneste(client, norg, oid, cache)
    }

    private companion object {
        private const val baseUrl = "http://localhost"
        private val GROUP_ID = randomUUID()
        private val OID = randomUUID()
        private val GRUPPE_OID = randomUUID()
        private val ANSATT_OID = randomUUID()
        private val ANSATT = Ansatt(AnsattId("E123456"), "Ola Nordmann", "Ola", "Nordmann")
        private val medlemId = "${randomUUID()}"
        private val gruppeMedlemmerContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#directoryObjects",
              "value": [
                {
                  "id": "$medlemId",
                  "displayName": "Ola Nordmann",
                  "givenName": "Ola",
                  "surname": "Nordmann",
                  "onPremisesSamAccountName": "E123456"
                }
              ]
            }
        """.trimIndent()

        private val oidContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#users",
              "value": [
                { "id": "$OID" }
              ]
            }
        """.trimIndent()

        private val gruppeContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#groups",
              "value": [
                { "id": "$GRUPPE_OID", "displayName": "En gruppe" }
              ]
            }
        """.trimIndent()

        private val temaContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#directoryObjects",
              "value": [
                { "id": "${randomUUID()}", "displayName": "0000-GA-TEMA_AAP" }
              ]
            }
        """.trimIndent()

        private val enhetContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#directoryObjects",
              "value": [
                { "id": "${randomUUID()}", "displayName": "0000-GA-ENHET_1234" }
              ]
            }
        """.trimIndent()

        private val grupperContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#directoryObjects",
              "value": [
                { "id": "${randomUUID()}", "displayName": "0000-GA-MIN_ROLLE" }
              ]
            }
        """.trimIndent()

        private val utvidetAnsattContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#users",
              "value": [
                {
                  "id": "${randomUUID()}",
                  "displayName": "Kari Nordmann",
                  "givenName": "Kari",
                  "surname": "Nordmann",
                  "jobTitle": "AAA1234",
                  "mail": "kari.nordmann@example.com",
                  "streetAddress": "1234",
                  "onPremisesSamAccountName": "A123456"
                }
              ]
            }
        """.trimIndent()
    }
}
