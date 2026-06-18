package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID.randomUUID

class EntraControllerTest : BehaviorSpec({

    val token: Token = mockk(relaxed = true)
    val entraAdapter: EntraRestClientAdapter = mockk()
    val oid: EntraOidTjeneste = mockk()
    val norg: NorgTjeneste = mockk(relaxed = true)
    val cache: ValkeyCacheOperations = mockk(relaxed = true)
    val entra = EntraTjeneste(entraAdapter, norg, oid, cache)
    val controller = EntraController(entra, oid, token)
    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    val jsonMapper: JsonMapper = JsonMapper.builder().findAndAddModules().build()

    beforeSpec {
        every { token.systemAndNs } returns "test:ns"
        every { token.systemNavn } returns "Test"
    }

    Given("enheter CC-endepunkt") {
        beforeContainer {
            every { token.erCC } returns true
            every { token.assert<Any>(any(), any()) } answers {
                @Suppress("UNCHECKED_CAST")
                (secondArg<() -> Set<Any>>())()
            }
        }

        When("ansatt har enheter") {
            Then("skal returnere enhetene") {
                val uuid = randomUUID()
                every { oid.ansattOid(ANSATTID) } returns uuid
                every { entraAdapter.enheter("$uuid") } returns setOf(ENHET.enhetnummer)
                every { norg.navnFor(ENHET.enhetnummer) } returns ENHET.navn

                val respons = mockMvc.perform(get("/api/v1/enhet/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                    .andReturn().response.contentAsString

                val enheter = jsonMapper.readValue<Set<Enhet>>(respons)
                enheter shouldHaveSize 1
                enheter.single() shouldBe ENHET
            }
        }

        When("ansatt ikke finnes i Entra") {
            Then("skal returnere tomt sett") {
                every { oid.ansattOid(ANSATTID) } returns null

                val respons = mockMvc.perform(get("/api/v1/enhet/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Enhet>>(respons).shouldBeEmpty()
            }
        }
    }

    Given("enheter OBO-endepunkt") {
        beforeContainer {
            every { token.erObo } returns true
            every { token.assert<Any>(any(), any()) } answers {
                @Suppress("UNCHECKED_CAST")
                (secondArg<() -> Set<Any>>())()
            }
        }

        When("ansatt har enheter via OBO") {
            Then("skal returnere enhetene") {
                val uuid = randomUUID()
                every { token.oboFields } returns (ANSATTID to uuid)
                every { entraAdapter.enheter("$uuid") } returns setOf(ENHET.enhetnummer)
                every { norg.navnFor(ENHET.enhetnummer) } returns ENHET.navn

                val respons = mockMvc.perform(get("/api/v1/enhet"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Enhet>>(respons).single() shouldBe ENHET
            }
        }
    }

    Given("tema CC-endepunkt") {
        beforeContainer {
            every { token.erCC } returns true
            every { token.assert<Any>(any(), any()) } answers {
                @Suppress("UNCHECKED_CAST")
                (secondArg<() -> Set<Any>>())()
            }
        }

        When("ansatt har tema-tilganger") {
            Then("skal returnere temaene") {
                val uuid = randomUUID()
                every { oid.ansattOid(ANSATTID) } returns uuid
                every { entraAdapter.tema("$uuid") } returns setOf(TEMA)

                val respons = mockMvc.perform(get("/api/v1/tema/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Tema>>(respons) shouldHaveSize 1
            }
        }

        When("ansatt ikke finnes") {
            Then("skal returnere tomt sett") {
                every { oid.ansattOid(ANSATTID) } returns null

                val respons = mockMvc.perform(get("/api/v1/tema/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Tema>>(respons).shouldBeEmpty()
            }
        }
    }

    Given("tema OBO-endepunkt") {
        beforeContainer {
            every { token.erObo } returns true
            every { token.assert<Any>(any(), any()) } answers {
                @Suppress("UNCHECKED_CAST")
                (secondArg<() -> Set<Any>>())()
            }
        }

        When("ansatt har tema-tilganger via OBO") {
            Then("skal returnere temaene") {
                val uuid = randomUUID()
                every { token.oboFields } returns (ANSATTID to uuid)
                every { entraAdapter.tema("$uuid") } returns setOf(TEMA)

                val respons = mockMvc.perform(get("/api/v1/tema"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Tema>>(respons) shouldHaveSize 1
            }
        }
    }

    Given("medlemmer for enhet-endepunkt") {
        When("enheten har medlemmer") {
            Then("skal returnere medlemmene") {
                val uuid = randomUUID()
                every { oid.gruppeOid(ENHET.enhetnummer.gruppeNavn) } returns uuid
                every { entraAdapter.gruppeMedlemmer("$uuid") } returns setOf(ANSATT)

                val respons = mockMvc.perform(get("/api/v1/enhet/${ENHET.enhetnummer.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Ansatt>>(respons).single() shouldBe ANSATT
            }
        }

        When("enheten ikke finnes") {
            Then("skal returnere tomt sett") {
                every { oid.gruppeOid(ENHET.enhetnummer.gruppeNavn) } returns null

                val respons = mockMvc.perform(get("/api/v1/enhet/${ENHET.enhetnummer.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Ansatt>>(respons).shouldBeEmpty()
            }
        }
    }

    Given("medlemmer for tema-endepunkt") {
        When("temaet har medlemmer") {
            Then("skal returnere medlemmene") {
                val uuid = randomUUID()
                every { oid.gruppeOid(TEMA.gruppeNavn) } returns uuid
                every { entraAdapter.gruppeMedlemmer("$uuid") } returns setOf(ANSATT)

                val respons = mockMvc.perform(get("/api/v1/tema/$AAP"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Ansatt>>(respons).single() shouldBe ANSATT
            }
        }
    }

    Given("ansatt-endepunkt med navIdent") {
        When("ansatt finnes") {
            Then("skal returnere utvidet ansattinformasjon") {
                every { entraAdapter.utvidetAnsatt(ANSATTID.verdi) } returns ANSATT_RESPONS
                every { norg.navnFor(any()) } returns "Enhet Navn"

                val respons = mockMvc.perform(get("/api/v1/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                respons.shouldNotBeNull()
            }
        }
    }

    Given("ansatt-endepunkt med tIdent") {
        When("ansatt finnes") {
            Then("skal returnere utvidet ansattinformasjon") {
                every { entraAdapter.utvidetAnsattTident(TIDENT.verdi) } returns ANSATT_RESPONS
                every { norg.navnFor(any()) } returns "Enhet Navn"

                val respons = mockMvc.perform(get("/api/v1/ansatt/tident/${TIDENT.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                respons.shouldNotBeNull()
            }
        }
    }

    Given("tilganger-endepunkt") {
        When("ansatt har grupper") {
            Then("skal returnere gruppene") {
                val uuid = randomUUID()
                every { oid.ansattOid(ANSATTID) } returns uuid
                every { entraAdapter.ansatteGrupper("$uuid") } returns setOf(GRUPPE)

                val respons = mockMvc.perform(get("/api/v1/ansatt/tilganger/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<EntraGruppe>>(respons).single() shouldBe GRUPPE
            }
        }

        When("ansatt ikke finnes") {
            Then("skal returnere null (tom respons)") {
                every { oid.ansattOid(ANSATTID) } returns null

                mockMvc.perform(get("/api/v1/ansatt/tilganger/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
            }
        }
    }

    Given("gruppe/medlemmer-endepunkt") {
        When("gruppe finnes") {
            Then("skal returnere medlemmene") {
                val uuid = randomUUID()
                val gruppeNavn = "0000-GA-ENHET_1234"
                every { oid.gruppeOid(gruppeNavn) } returns uuid
                every { entraAdapter.gruppeMedlemmer("$uuid") } returns setOf(ANSATT)

                val respons = mockMvc.perform(get("/api/v1/gruppe/medlemmer").param("gruppeNavn", gruppeNavn))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Ansatt>>(respons).single() shouldBe ANSATT
            }
        }
    }
}) {
    companion object {
        const val AAP = "AAP"
        val ANSATTID = AnsattId("A123456")
        val TIDENT = TIdent("AAA1234")
        val TEMA = Tema(AAP)
        val ANSATT = Ansatt(AnsattId("E123456"), "Ola Nordmann", "Ola", "Nordmann")
        val ENHET = Enhet(Enhetnummer("1234"), "Enhet Navn")
        val GRUPPE = EntraGruppe("0000-GA-ENHET_1234")
        val ANSATT_RESPONS = no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons(
            id = randomUUID(),
            onPremisesSamAccountName = "A123456",
            displayName = "Ola Nordmann",
            givenName = "Ola",
            surname = "Nordmann",
            jobTitle = "AAA1234",
            mail = "ola@nav.no",
            streetAddress = "1234"
        )
    }
}


