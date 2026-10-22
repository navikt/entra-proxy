package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheInaktiveAnsatte
import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheTestConfig
import no.nav.sikkerhetstjenesten.entraproxy.cache.CaffeineCacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.ENTRA_OID
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.UnorderedRequestExpectationManager
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter.create
import org.springframework.web.service.invoker.HttpServiceProxyFactory.builderFor
import org.springframework.web.service.invoker.createClient
import java.util.UUID
import java.util.UUID.randomUUID

@RestClientTest
class EntraTjenesteTest(
    private val server: MockRestServiceServer,
    private val client: EntraGraphClient,
    private val entra: EntraTjeneste,
    private val adapter: EntraGraphAdapter,
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

        Given("antall medlemmer for flere grupper via batch") {
            When("det er flere grupper enn det får plass til i én batch") {
                Then("deles kallene i batcher på 20 og resultatet slås sammen") {
                    val gruppeIder = (1..25).map { randomUUID() }
                    server.expect { request ->
                        request.method shouldBe POST
                        request.uri.toString() shouldBe $$"$$baseUrl/$batch"
                    }.andRespond(withSuccess(batchRespons(gruppeIder.take(20)), APPLICATION_JSON))
                    server.expect { request ->
                        request.method shouldBe POST
                        request.uri.toString() shouldBe $$"$$baseUrl/$batch"
                    }.andRespond(withSuccess(batchRespons(gruppeIder.drop(20)), APPLICATION_JSON))

                    val resultat = entra.antallMedlemmerIGrupper(*gruppeIder.toTypedArray())

                    resultat.size shouldBe 25
                    resultat shouldBe gruppeIder.associateWith { 1L }
                }
                Then("sendes ConsistencyLevel: eventual på hver delforespørsel") {
                    val gruppeIder = (1..1).map { randomUUID() }
                    server.expect { request ->
                        request.method shouldBe POST
                        request.uri.toString() shouldBe $$"$$baseUrl/$batch"
                        (request as MockClientHttpRequest).bodyAsString shouldContain
                            """"headers":{"ConsistencyLevel":"eventual"}"""
                    }.andRespond(withSuccess(batchRespons(gruppeIder), APPLICATION_JSON))

                    entra.antallMedlemmerIGrupper(*gruppeIder.toTypedArray())
                }
            }
            When("én delforespørsel feiler med et feilobjekt som body") {
                Then("hoppes den gruppen over uten å feile hele batchen") {
                    val gruppeIder = (1..2).map { randomUUID() }
                    server.expect { request ->
                        request.method shouldBe POST
                        request.uri.toString() shouldBe $$"$$baseUrl/$batch"
                    }.andRespond(
                        withSuccess(
                            """
                            {
                              "responses": [
                                { "id": "0", "status": 200, "body": 3 },
                                { "id": "1", "status": 404, "body": { "error": { "code": "ResourceNotFound" } } }
                              ]
                            }
                            """.trimIndent(),
                            APPLICATION_JSON
                        )
                    )

                    val resultat = entra.antallMedlemmerIGrupper(*gruppeIder.toTypedArray())

                    resultat shouldBe mapOf(gruppeIder[0] to 3L)
                }
            }
        }

        Given("medlemmer-endepunkt") {
            When("det finnes medlemmer") {
                Then("skal responsen inneholde forventede medlemmer") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/groups/$GROUP_ID/members")
                    }.andRespond(withSuccess(gruppeMedlemmerContract, APPLICATION_JSON))

                    entra.medlemmerIGruppe( GROUP_ID) shouldBe setOf(ANSATT)
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

                    adapter.gruppeMedlemmer("$gruppeId") shouldBe setOf(
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

                    adapter.gruppeMedlemmer("$gruppeId") shouldBe setOf(ANSATT)
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

        Given("groups-endepunkt for søk etter grupper som matcher en substring") {
            When("flere grupper matcher og søket pagineres via @odata.nextLink") {
                Then("skal medlemmene i alle matchende grupper returneres med gruppenavn") {
                    val gruppe1Id = randomUUID()
                    val gruppe2Id = randomUUID()
                    val nesteGruppeSideUri = "$baseUrl/groups?\$skiptoken=grp"

                    server.expect { request ->
                        request.method shouldBe GET
                        request.uri.path shouldBe "/groups"
                        request.uri.query shouldContain "\$search=\"displayName:MIN_ROLLE\""
                    }.andRespond(withSuccess(
                        """
                        {
                          "@odata.nextLink": "$nesteGruppeSideUri",
                          "value": [
                            { "id": "$gruppe1Id", "displayName": "0000-GA-MIN_ROLLE_A", "description": "A" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))

                    server.expect { request ->
                        request.method == GET && request.uri.toString() == nesteGruppeSideUri
                    }.andRespond(withSuccess(
                        """
                        {
                          "value": [
                            { "id": "$gruppe2Id", "displayName": "0000-GA-MIN_ROLLE_B", "description": "B" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))

                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/groups/$gruppe1Id/members")
                    }.andRespond(withSuccess(gruppeMedlemmerContract, APPLICATION_JSON))
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/groups/$gruppe2Id/members")
                    }.andRespond(withSuccess(
                        """
                        {
                          "value": [
                            { "id": "${randomUUID()}", "displayName": "Kari Nordmann", "givenName": "Kari", "surname": "Nordmann", "onPremisesSamAccountName": "E654321" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))

                    entra.medlemmerIGrupperSomMatcher("MIN_ROLLE") shouldBe listOf(
                        GruppeMedMedlemmer(gruppe1Id, "0000-GA-MIN_ROLLE_A", setOf(ANSATT)),
                        GruppeMedMedlemmer(gruppe2Id, "0000-GA-MIN_ROLLE_B",
                            setOf(Ansatt(AnsattId("E654321"), "Kari Nordmann", "Kari", "Nordmann")))
                    )
                }
            }
            When("samme ansatt er medlem i flere matchende grupper") {
                Then("skal resultatet per ansatt samle alle gruppene vedkommende er medlem i") {
                    val gruppe1Id = randomUUID()
                    val gruppe2Id = randomUUID()
                    val nesteGruppeSideUri = "$baseUrl/groups?\$skiptoken=grp"

                    server.expect { request ->
                        request.method shouldBe GET
                        request.uri.path shouldBe "/groups"
                    }.andRespond(withSuccess(
                        """
                        {
                          "@odata.nextLink": "$nesteGruppeSideUri",
                          "value": [
                            { "id": "$gruppe1Id", "displayName": "0000-GA-MIN_ROLLE_A" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))
                    server.expect { request ->
                        request.method == GET && request.uri.toString() == nesteGruppeSideUri
                    }.andRespond(withSuccess(
                        """
                        {
                          "value": [
                            { "id": "$gruppe2Id", "displayName": "0000-GA-MIN_ROLLE_B" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))

                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/groups/$gruppe1Id/members")
                    }.andRespond(withSuccess(gruppeMedlemmerContract, APPLICATION_JSON))
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/groups/$gruppe2Id/members")
                    }.andRespond(withSuccess(gruppeMedlemmerContract, APPLICATION_JSON))

                    entra.grupperPerAnsattSomMatcher("MIN_ROLLE") shouldBe listOf(
                        AnsattMedGrupper(ANSATT, setOf("0000-GA-MIN_ROLLE_A", "0000-GA-MIN_ROLLE_B"))
                    )
                }
            }
        }

        Given("memberOf-endepunkt for temaer") {
            When("ansatt er medlem av en tema-gruppe") {
                Then("skal temaet returneres") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users?")
                    }.andRespond(withSuccess(ansattOidContract, APPLICATION_JSON))
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users/$ANSATT_OID/memberOf")
                    }.andRespond(withSuccess(temaContract, APPLICATION_JSON))

                    repeat(2) {
                        entra.tema(AnsattId("B123456")) shouldBe setOf(Tema("AAP"))
                    }
                }
            }
        }

        Given("memberOf-endepunkt for enheter") {
            When("ansatt er medlem av en enhet-gruppe") {
                Then("skal enhetsnummeret returneres") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users?")
                    }.andRespond(withSuccess(ansattOidContract, APPLICATION_JSON))
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users/$ANSATT_OID/memberOf")
                    }.andRespond(withSuccess(enhetContract, APPLICATION_JSON))

                    repeat(2) {
                        entra.enheter(AnsattId("C123456")).map { it.enhetnummer.verdi } shouldBe listOf("1234")
                    }
                }
            }
        }

        Given("memberOf-endepunkt for grupper") {
            When("ansatt er medlem av en gruppe") {
                Then("skal gruppen returneres") {
                    server.expect { request ->
                        request.method == GET && request.uri.toString().startsWith("$baseUrl/users?")
                    }.andRespond(withSuccess(ansattOidContract, APPLICATION_JSON))
                    server.expect { request ->
                        request.method shouldBe GET
                        request.uri.path shouldBe "/users/$ANSATT_OID/memberOf/microsoft.graph.group"
                        request.uri.query shouldContain "\$select=id,displayName,securityEnabled,mailEnabled,description"
                    }.andRespond(withSuccess(grupperContract, APPLICATION_JSON))

                    repeat(2) {
                        entra.grupper(AnsattId("D123456")) shouldBe
                            setOf(EntraGruppe("0000-GA-MIN_ROLLE", GRUPPE_ID, "Tilgang til min rolle"))
                    }
                }
            }
            When("gruppen mangler beskrivelse") {
                Then("skal gruppen returneres uten beskrivelse") {
                    server.expect { request ->
                        request.method shouldBe GET
                    }.andRespond(withSuccess(grupperContract.replace(", \"description\": \"Tilgang til min rolle\"", ""), APPLICATION_JSON))

                    adapter.grupper(ANSATT_OID) shouldBe
                        setOf(EntraGruppe("0000-GA-MIN_ROLLE", GRUPPE_ID))
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
        fun mockServerRestClientCustomizer() =
            MockServerRestClientCustomizer(UnorderedRequestExpectationManager::class.java)

        @Bean
        fun entraGraphClient(builder: RestClient.Builder): EntraGraphClient =
            builderFor(create(builder.baseUrl(baseUrl).build())).build().createClient()

        @Bean
        fun entraOidTjeneste(client: EntraGraphClient) = EntraOidTjeneste(client)

        @Bean
        fun entraGraphAdapter(client: EntraGraphClient) = EntraGraphAdapter(client)

        @Bean
        fun oidOppslag(oid: EntraOidTjeneste, cache: CaffeineCacheOperations): EntraOidOperations = EntraOidOppslag(oid, cache)

        @Bean
        fun inaktiveAnsatte(cache: CaffeineCacheOperations): InaktiveAnsatte = CacheInaktiveAnsatte(cache)

        @Bean
        @Primary
        fun entraTjeneste(adapter: EntraGraphAdapter, inaktive: InaktiveAnsatte, oid: EntraOidOperations, enhetsnavn: NorgTjeneste) =
            EntraTjeneste(adapter, inaktive, oid, enhetsnavn)
    }

    private companion object {
        private const val baseUrl = "http://localhost"
        private val GROUP_ID = randomUUID()
        private val OID = randomUUID()
        private val GRUPPE_OID = randomUUID()
        private val GRUPPE_ID = randomUUID()
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

        private val ansattOidContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#users",
              "value": [
                { "id": "$ANSATT_OID" }
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

        private fun batchRespons(gruppeIder: List<UUID>) = """
            {
              "responses": [
                ${gruppeIder.mapIndexed { index, _ -> """{ "id": "$index", "status": 200, "body": 1 }""" }.joinToString(",\n                ")}
              ]
            }
        """.trimIndent()

        private val grupperContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#directoryObjects",
              "value": [
                { "id": "$GRUPPE_ID", "displayName": "0000-GA-MIN_ROLLE", "description": "Tilgang til min rolle" }
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
