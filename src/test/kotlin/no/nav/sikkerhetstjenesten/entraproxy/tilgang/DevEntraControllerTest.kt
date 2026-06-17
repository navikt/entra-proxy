package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons
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

class DevEntraControllerTest : BehaviorSpec({

    val entraAdapter: EntraRestClientAdapter = mockk()
    val oid: EntraOidTjeneste = mockk()
    val norg: NorgTjeneste = mockk(relaxed = true)
    val cache: ValkeyCacheOperations = mockk(relaxed = true)
    val entra = EntraTjeneste(entraAdapter, norg, oid, cache)
    val controller = DevEntraController(entra, oid, norg)
    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    val jsonMapper: JsonMapper = JsonMapper.builder().findAndAddModules().build()

    Given("enheter for ansatt") {
        When("ansatt har enheter") {
            Then("skal returnere enhetene") {
                val uuid = randomUUID()
                every { oid.ansattOid(ANSATTID) } returns uuid
                every { entraAdapter.enheter("$uuid") } returns setOf(ENHET.enhetnummer)
                every { norg.navnFor(ENHET.enhetnummer) } returns ENHET.navn

                val respons = mockMvc.perform(get("/dev/enhet/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Enhet>>(respons).single() shouldBe ENHET
            }
        }

        When("ansatt ikke finnes") {
            Then("skal returnere null") {
                every { oid.ansattOid(ANSATTID) } returns null

                mockMvc.perform(get("/dev/enhet/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
            }
        }
    }

    Given("tema for ansatt") {
        When("ansatt har tema-tilganger") {
            Then("skal returnere temaene") {
                val uuid = randomUUID()
                every { oid.ansattOid(ANSATTID) } returns uuid
                every { entraAdapter.tema("$uuid") } returns setOf(TEMA)

                val respons = mockMvc.perform(get("/dev/tema/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Tema>>(respons) shouldHaveSize 1
            }
        }
    }

    Given("tilganger for ansatt") {
        When("ansatt har grupper") {
            Then("skal returnere gruppene") {
                val uuid = randomUUID()
                every { oid.ansattOid(ANSATTID) } returns uuid
                every { entraAdapter.ansatteGrupper("$uuid") } returns setOf(GRUPPE)

                val respons = mockMvc.perform(get("/dev/tilganger/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<EntraGruppe>>(respons).single() shouldBe GRUPPE
            }
        }
    }

    Given("medlemmer for enhet") {
        When("enheten har medlemmer") {
            Then("skal returnere medlemmene") {
                val uuid = randomUUID()
                every { oid.gruppeOid(ENHET.enhetnummer.gruppeNavn) } returns uuid
                every { entraAdapter.gruppeMedlemmer("$uuid") } returns setOf(ANSATT)

                val respons = mockMvc.perform(get("/dev/enhet/${ENHET.enhetnummer.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Ansatt>>(respons).single() shouldBe ANSATT
            }
        }
    }

    Given("norg navn for enhet") {
        When("enheten finnes i Norg") {
            Then("skal returnere navnet") {
                every { norg.navnFor(ENHET.enhetnummer) } returns "NAV Drammen"

                val respons = mockMvc.perform(get("/dev/navn/${ENHET.enhetnummer.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                respons shouldBe "NAV Drammen"
            }
        }
    }

    Given("medlemmer for tema") {
        When("temaet har medlemmer") {
            Then("skal returnere medlemmene") {
                val uuid = randomUUID()
                every { oid.gruppeOid(TEMA.gruppeNavn) } returns uuid
                every { entraAdapter.gruppeMedlemmer("$uuid") } returns setOf(ANSATT)

                val respons = mockMvc.perform(get("/dev/tema/$AAP"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Ansatt>>(respons).single() shouldBe ANSATT
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

                val respons = mockMvc.perform(get("/dev/gruppe/medlemmer").param("gruppeNavn", gruppeNavn))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                jsonMapper.readValue<Set<Ansatt>>(respons).single() shouldBe ANSATT
            }
        }
    }

    Given("utvidet ansatt med navIdent") {
        When("ansatt finnes") {
            Then("skal returnere ansattinformasjon") {
                every { entraAdapter.utvidetAnsatt(ANSATTID.verdi) } returns ANSATT_RESPONS
                every { norg.navnFor(any()) } returns "Enhet Navn"

                val respons = mockMvc.perform(get("/dev/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                respons.shouldNotBeNull()
            }
        }
    }

    Given("utvidet ansatt med tIdent") {
        When("ansatt finnes") {
            Then("skal returnere ansattinformasjon") {
                every { entraAdapter.utvidetAnsattTident(TIDENT.verdi) } returns ANSATT_RESPONS
                every { norg.navnFor(any()) } returns "Enhet Navn"

                val respons = mockMvc.perform(get("/dev/ansatt/tident/${TIDENT.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                respons.shouldNotBeNull()
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
        val ANSATT_RESPONS = EntraSaksbehandlerRespons.AnsattRespons(
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

