package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheInaktiveAnsatte
import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheTestConfig
import no.nav.sikkerhetstjenesten.entraproxy.cache.CaffeineCacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.ENTRA_OID
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.felles.rest.DefaultRestErrorHandler
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.UnorderedRequestExpectationManager
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter.create
import org.springframework.web.service.invoker.HttpServiceProxyFactory.builderFor
import org.springframework.web.service.invoker.createClient
import java.util.UUID
import java.util.UUID.randomUUID

@RestClientTest
class EntraTjenesteRestClientTest(
    private val server: MockRestServiceServer,
    private val entra: EntraTjeneste,
    private val adapter: EntraGraphAdapter,
    private val oid: EntraOidTjeneste,
    private val oidOppslag: EntraOidOperations,
    private val cache: CaffeineCacheOperations) : BehaviorSpec() {

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

                    assertSoftly {
                        resultat.size shouldBe 25
                        resultat shouldBe gruppeIder.associateWith { 1L }
                    }
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
                    expectGet("$baseUrl/groups/$GROUP_ID/members", gruppeMedlemmerContract)

                    entra.medlemmerIGruppe( GROUP_ID) shouldBe setOf(ANSATT)
                }
            }
        }

        Given("medlemmer-endepunkt med paginering") {
            When("Graph svarer med flere sider via @odata.nextLink") {
                Then("skal alle sidene hentes og medlemmene fra alle sidene returneres") {
                    val gruppeId = randomUUID()
                    val nesteSideUri = $$"$$baseUrl/groups/$$gruppeId/members?$skiptoken=abc"

                    expectGet(
                        "$baseUrl/groups/$gruppeId/members",
                        """
                        {
                          "@odata.nextLink": "$nesteSideUri",
                          "value": [
                            $olaMedlemJson
                          ]
                        }
                        """.trimIndent()
                    )

                    server.expect { request ->
                        request.method shouldBe GET; request.uri.toString() shouldBe nesteSideUri
                    }.andRespond(withSuccess(
                        """
                        {
                          "value": [
                            $kariMedlemJson
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

                    expectGet(
                        "$baseUrl/groups/$gruppeId/members",
                        """
                        {
                          "value": [
                            $olaMedlemJson,
                            { "id": "$nøstetGruppeId", "displayName": "En nøstet gruppe" }
                          ]
                        }
                        """.trimIndent()
                    )

                    adapter.gruppeMedlemmer("$gruppeId") shouldBe setOf(ANSATT)
                }
            }
        }

        Given("users-endepunkt for oppslag av oid") {
            When("det finnes nøyaktig én bruker for navident") {
                Then("skal oid returneres") {
                    expectGet("$baseUrl/users", oidRespons(OID))
                    repeat(2) {
                        oid.ansattOid(AnsattId("A123456")) shouldBe OID
                    }
                }
            }
            When("en utdatert OID ligger i cache") {
                Then("erstatter oppfrisking den med OID fra Graph") {
                    val ident = AnsattId("F123456")
                    val gammelOid = randomUUID()
                    val nyOid = randomUUID()
                    cache.putOne(OID_CACHE, ident.verdi, gammelOid)
                    oidOppslag.ansattOid(ident) shouldBe gammelOid
                    expectGet("$baseUrl/users", oidRespons(nyOid))

                    val oppfrisket = oidOppslag.oppfriskOid(ident)
                    val cachet = oidOppslag.ansattOid(ident)

                    assertSoftly {
                        oppfrisket shouldBe nyOid
                        cachet shouldBe nyOid
                    }
                }
            }
        }

        Given("en cachet OID som ikke lenger finnes i Graph") {
            When("Graph returnerer HTTP-404 ved gruppeoppslag") {
                Then("oppfriskes OID og gruppene hentes med den nye verdien") {
                    val ident = AnsattId("L123456")
                    val gammelOid = randomUUID()
                    val nyOid = randomUUID()
                    cache.putOne(OID_CACHE, ident.verdi, gammelOid)
                    oidOppslag.ansattOid(ident) shouldBe gammelOid
                    server.expect { request ->
                        request.method shouldBe GET
                        request.uri.path shouldBe "/users/$gammelOid/memberOf/microsoft.graph.group"
                    }.andRespond(withResourceNotFound())
                    server.expect { request ->
                        request.method shouldBe GET
                        request.uri.path shouldBe "/users"
                        request.uri.query shouldContain "\$filter=onPremisesSamAccountName eq '${ident.verdi}'"
                    }.andRespond(withSuccess(oidRespons(nyOid), APPLICATION_JSON))
                    server.expect { request ->
                        request.method shouldBe GET
                        request.uri.path shouldBe "/users/$nyOid/memberOf/microsoft.graph.group"
                    }.andRespond(withSuccess(grupperContract, APPLICATION_JSON))

                    val resultat = entra.grupper(ident)
                    val cachetResultat = entra.grupper(ident)
                    val cachetOid = oidOppslag.ansattOid(ident)

                    assertSoftly {
                        resultat shouldBe setOf(EntraGruppe("0000-GA-MIN_ROLLE", GRUPPE_ID, "Tilgang til min rolle"))
                        cachetResultat shouldBe resultat
                        cachetOid shouldBe nyOid
                    }
                }
            }
        }

        Given("OID-oppslag uten et entydig brukertreff") {
            When("Graph ikke finner noen bruker") {
                Then("kastes EntraIkkeFunnetException med riktig navident") {
                    val ident = AnsattId("G123456")
                    expectGet("$baseUrl/users", """{"value":[]}""")

                    val feil = shouldThrow<EntraIkkeFunnetException> { oid.ansattOid(ident) }

                    feil.ansattId shouldBe ident
                }
            }

            When("Graph finner flere brukere") {
                Then("kastes EntraOidException med navident og antall treff") {
                    val ident = AnsattId("H123456")
                    val id1 = randomUUID()
                    val id2 = randomUUID()
                    expectGet("$baseUrl/users", """{"value":[{"id":"$id1"},{"id":"$id2"}]}""")

                    val feil = shouldThrow<EntraOidException> { oid.ansattOid(ident) }

                    assertSoftly {
                        feil.statusCode.value() shouldBe 404
                        feil.body.title shouldBe "Uventet respons fra Entra"
                        feil.body.properties?.get("navIdent") shouldBe ident.verdi
                        feil.body.detail.orEmpty() shouldContain "fant 2"
                    }
                }
            }
        }

        Given("groups-endepunkt for oppslag av gruppe-oid") {
            When("det finnes en gruppe med gitt navn") {
                Then("skal gruppens oid returneres") {
                    expectGet("$baseUrl/groups", gruppeContract)

                    repeat(2) {
                        oid.gruppeOid("En gruppe") shouldBe GRUPPE_OID
                    }
                }
            }
        }

        Given("oppslag av en ukjent gruppe") {
            When("Graph ikke finner noen gruppe") {
                Then("returneres null") {
                    expectGet("$baseUrl/groups", """{"value":[]}""")

                    oid.gruppeOid("UKJENT_GRUPPE") shouldBe null
                }
            }
        }

        Given("groups-endepunkt for søk etter grupper som matcher en substring") {
            When("flere grupper matcher og søket pagineres via @odata.nextLink") {
                Then("skal medlemmene i alle matchende grupper returneres med gruppenavn") {
                    val gruppe1Id = randomUUID()
                    val gruppe2Id = randomUUID()
                    val nesteGruppeSideUri = $$"$$baseUrl/groups?$skiptoken=grp"

                    server.expect { request ->
                        request.method shouldBe GET
                        request.uri.path shouldBe "/groups"
                        request.uri.query shouldContain $$"$search=\"displayName:MIN_ROLLE\""
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
                        request.method shouldBe GET; request.uri.toString() shouldBe nesteGruppeSideUri
                    }.andRespond(withSuccess(
                        """
                        {
                          "value": [
                            { "id": "$gruppe2Id", "displayName": "0000-GA-MIN_ROLLE_B", "description": "B" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))

                    expectGet("$baseUrl/groups/$gruppe1Id/members", gruppeMedlemmerContract)
                    expectGet(
                        "$baseUrl/groups/$gruppe2Id/members",
                        """
                        {
                          "value": [
                            $kariMedlemJson
                          ]
                        }
                        """.trimIndent()
                    )

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
                        request.method shouldBe GET; request.uri.toString() shouldBe nesteGruppeSideUri
                    }.andRespond(withSuccess(
                        """
                        {
                          "value": [
                            { "id": "$gruppe2Id", "displayName": "0000-GA-MIN_ROLLE_B" }
                          ]
                        }
                        """.trimIndent(), APPLICATION_JSON))

                    expectGet("$baseUrl/groups/$gruppe1Id/members", gruppeMedlemmerContract)
                    expectGet("$baseUrl/groups/$gruppe2Id/members", gruppeMedlemmerContract)

                    entra.grupperPerAnsattSomMatcher("MIN_ROLLE") shouldBe listOf(
                        AnsattMedGrupper(ANSATT, setOf("0000-GA-MIN_ROLLE_A", "0000-GA-MIN_ROLLE_B"))
                    )
                }
            }
        }

        Given("memberOf-endepunkt for temaer") {
            When("ansatt er medlem av en tema-gruppe") {
                Then("skal temaet returneres") {
                    expectGet("$baseUrl/users?", oidRespons(ANSATT_OID))
                    expectGet("$baseUrl/users/$ANSATT_OID/memberOf", temaContract)

                    repeat(2) {
                        entra.tema(AnsattId("B123456")) shouldBe setOf(Tema("AAP"))
                    }
                }
            }
        }

        Given("memberOf-endepunkt for enheter") {
            When("ansatt er medlem av en enhet-gruppe") {
                Then("skal enhetsnummeret returneres") {
                    expectGet("$baseUrl/users?", oidRespons(ANSATT_OID))
                    expectGet("$baseUrl/users/$ANSATT_OID/memberOf", enhetContract)

                    repeat(2) {
                        entra.enheter(AnsattId("C123456")).map { it.enhetnummer.verdi } shouldBe listOf("1234")
                    }
                }
            }
        }

        Given("memberOf-endepunkt for grupper") {
            When("ansatt er medlem av en gruppe") {
                Then("skal gruppen returneres") {
                    expectGet("$baseUrl/users?", oidRespons(ANSATT_OID))
                    server.expect { request ->
                        request.method shouldBe GET
                        request.uri.path shouldBe "/users/$ANSATT_OID/memberOf/microsoft.graph.group"
                        request.uri.query shouldContain $$"$select=id,displayName,securityEnabled,mailEnabled,description"
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

        Given("varianter av oppslag for utvidet ansatt") {
            When("oppslaget bruker TIdent") {
                Then("brukes jobTitle-filteret og riktig ansatt returneres") {
                    val ident = TIdent("BCD1234")
                    server.expect { request ->
                        request.method shouldBe GET
                        request.uri.path shouldBe "/users"
                        request.uri.query shouldContain "\$filter=jobTitle eq 'BCD1234'"
                    }.andRespond(withSuccess(utvidetAnsattContract.replace("AAA1234", "BCD1234"), APPLICATION_JSON))

                    val respons = entra.utvidetAnsatt(ident)

                    assertSoftly {
                        respons?.navIdent shouldBe AnsattId("A123456")
                        respons?.tIdent shouldBe ident
                        respons?.fornavn shouldBe "Kari"
                        respons?.enhet?.enhetnummer?.verdi shouldBe "1234"
                    }
                }
            }

            When("Graph ikke finner noen bruker") {
                Then("returneres null") {
                    expectGet("$baseUrl/users", """{"value":[]}""")

                    entra.utvidetAnsatt(AnsattId("I123456")) shouldBe null
                }
            }

            When("brukeren mangler navident") {
                Then("returneres null i stedet for en ufullstendig ansatt") {
                    expectGet("$baseUrl/users", """{"value":[{"id":"${randomUUID()}","displayName":"Test Ansatt"}]}""")

                    entra.utvidetAnsatt(AnsattId("J123456")) shouldBe null
                }
            }

            When("brukeren mangler streetAddress og jobTitle") {
                Then("brukes standardverdier for enhetsnummer og TIdent") {
                    val ident = AnsattId("K123456")
                    expectGet(
                        "$baseUrl/users",
                        """{"value":[{"id":"${randomUUID()}","onPremisesSamAccountName":"${ident.verdi}"}]}"""
                    )

                    val respons = entra.utvidetAnsatt(ident)

                    assertSoftly {
                        respons?.navIdent shouldBe ident
                        respons?.tIdent shouldBe TIdent("AAA1234")
                        respons?.enhet?.enhetnummer?.verdi shouldBe "0000"
                    }
                }
            }
        }

        Given("users-endepunkt for utvidet ansatt") {
            When("det finnes en bruker for navident") {
                Then("skal utvidet ansatt-informasjon returneres") {
                    expectGet("$baseUrl/users", utvidetAnsattContract)

                    repeat(2) {
                        val respons = entra.utvidetAnsatt(AnsattId("A123456"))
                        assertSoftly {
                            respons?.navIdent shouldBe AnsattId("A123456")
                            respons?.fornavn shouldBe "Kari"
                            respons?.etternavn shouldBe "Nordmann"
                            respons?.enhet?.enhetnummer?.verdi shouldBe "1234"
                        }
                    }
                }
            }
        }
    }

    private fun expectGet(urlPrefix: String, responseJson: String) {
        server.expect { request ->
            request.method shouldBe GET
            request.uri.toString() shouldStartWith urlPrefix
        }.andRespond(withSuccess(responseJson, APPLICATION_JSON))
    }

    @TestConfiguration(proxyBeanMethods = false)
    class EntraTestConfig : CacheTestConfig(MEDLEMMER, GRAPH, ENTRA_OID) {
        @Bean
        fun mockServerRestClientCustomizer() =
            MockServerRestClientCustomizer(UnorderedRequestExpectationManager::class.java)

        @Bean
        fun entraGraphClient(builder: RestClient.Builder): EntraGraphClient =
            builderFor(create(builder
                .baseUrl(baseUrl)
                .defaultStatusHandler(HttpStatusCode::isError, DefaultRestErrorHandler()::handle)
                .build())).build().createClient()

        @Bean
        fun entraOidTjeneste(client: EntraGraphClient) = EntraOidTjeneste(client)

        @Bean
        fun entraGraphAdapter(client: EntraGraphClient) = EntraGraphAdapter(client)

        @Bean
        fun oidOppslag(oid: EntraOidTjeneste, cache: CaffeineCacheOperations): EntraOidOperations = EntraOidOppslag(oid, cache)

        @Bean
        fun inaktiveAnsatte(cache: CaffeineCacheOperations): InaktivOperations = CacheInaktiveAnsatte(cache)

        @Bean
        @Primary
        fun entraTjeneste(adapter: EntraGraphAdapter, inaktive: InaktivOperations, oid: EntraOidOperations, enhetsnavn: NorgTjeneste) =
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
        private val olaMedlemJson = """
            {
              "id": "${randomUUID()}",
              "displayName": "Ola Nordmann",
              "givenName": "Ola",
              "surname": "Nordmann",
              "onPremisesSamAccountName": "E123456"
            }
        """.trimIndent()

        private val kariMedlemJson = """
            {
              "id": "${randomUUID()}",
              "displayName": "Kari Nordmann",
              "givenName": "Kari",
              "surname": "Nordmann",
              "onPremisesSamAccountName": "E654321"
            }
        """.trimIndent()

        private val gruppeMedlemmerContract = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#directoryObjects",
              "value": [
                $olaMedlemJson
              ]
            }
        """.trimIndent()

        private fun oidRespons(id: UUID) = """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/${'$'}metadata#users",
              "value": [
                { "id": "$id" }
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
