package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.util.UUID.randomUUID

class TilgangTester : BehaviorSpec({

    val restDocumentation = ManualRestDocumentation()
    val token: Token = mockk(relaxed = true)
    val entraAdapter: EntraRestClientAdapter = mockk()
    val oid: EntraOidTjeneste = mockk()
    val norg: NorgTjeneste = mockk(relaxed = true)
    val cache: ValkeyCacheOperations = mockk(relaxed = true)
    val entra = EntraTjeneste(entraAdapter, norg, oid, cache)
    val controller = EntraController(entra, oid, token)
    val mockMvc: MockMvc = standaloneSetup(controller)
        .apply<StandaloneMockMvcBuilder>(documentationConfiguration(restDocumentation))
        .build()
    val jsonMapper: JsonMapper = JsonMapper.builder().findAndAddModules().build()

    beforeSpec {
        every { token.systemAndNs } returns "test:ns"
        every { token.systemNavn } returns "Test"
        every { token.erCC } returns true
        every { token.assert<Any>(any(), any()) } answers {
            @Suppress("UNCHECKED_CAST")
            (secondArg<() -> Set<Any>>())()
        }
    }

    beforeEach {
        clearMocks(norg, answers = false)
        restDocumentation.beforeTest(TilgangTester::class.java, it.name.name)
    }

    afterEach {
        restDocumentation.afterTest()
    }

    Given("tema-endepunkt") {
        When("det finnes medlemmer") {
            Then("skal responsen inneholde forventet ansatt") {
                every { oid.gruppeOid(TEMA.gruppeNavn) } returns UUID
                every { entraAdapter.gruppeMedlemmer("$UUID") } returns setOf(ansatt)
                val respons = mockMvc.perform(get("/api/v1/tema/$AAP"))
                    .andExpect(status().isOk)
                    .andDo(document("tema/medlemmer", preprocessResponse(prettyPrint())))
                    .andReturn().response.contentAsString
                jsonMapper.readValue<Set<Ansatt>>(respons).single() shouldBe ansatt
            }
        }
    }

    Given("enhet-endepunkt") {
        When("adapter feiler første gang og lykkes etter refreshOid") {
            Then("skal responsen inneholde forventet enhet") {
                every { oid.ansattOid(ANSATTID) } returns UUID
                every { entraAdapter.enheter("$UUID") } throws
                    NotFoundRestException(URI.create(""), "ikke funnet") andThen setOf(ENHET.enhetnummer)
                every { norg.navnFor(ENHET.enhetnummer) } returns ENHET.navn
                val respons = mockMvc.perform(get("/api/v1/enhet/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andDo(document("enhet/ansatt", preprocessResponse(prettyPrint())))
                    .andReturn().response.contentAsString
                jsonMapper.readValue<Set<Enhet>>(respons).single() shouldBe ENHET
                verify(exactly = 2)  { entraAdapter.enheter("$UUID") }
            }
        }
    }

    Given("enheter OBO-endepunkt") {
        When("ansatt har enheter via OBO") {
            Then("skal dokumentere responsen") {
                every { token.erObo } returns true
                every { token.assert<Any>(any(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    (secondArg<() -> Set<Any>>())()
                }
                every { token.oboFields } returns (ANSATTID to UUID)
                every { entraAdapter.enheter("$UUID") } returns setOf(ENHET.enhetnummer)
                every { norg.navnFor(ENHET.enhetnummer) } returns ENHET.navn

                mockMvc.perform(get("/api/v1/enhet"))
                    .andExpect(status().isOk)
                    .andDo(document("enhet/obo", preprocessResponse(prettyPrint())))
            }
        }
    }

    Given("tema CC-endepunkt") {
        When("ansatt har tema-tilganger") {
            Then("skal dokumentere responsen") {
                every { oid.ansattOid(ANSATTID) } returns UUID
                every { entraAdapter.tema("$UUID") } returns setOf(TEMA)

                mockMvc.perform(get("/api/v1/tema/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andDo(document("tema/ansatt", preprocessResponse(prettyPrint())))
            }
        }
    }

    Given("tema OBO-endepunkt") {
        When("ansatt har tema-tilganger via OBO") {
            Then("skal dokumentere responsen") {
                every { token.erObo } returns true
                every { token.assert<Any>(any(), any()) } answers {
                    @Suppress("UNCHECKED_CAST")
                    (secondArg<() -> Set<Any>>())()
                }
                every { token.oboFields } returns (ANSATTID to UUID)
                every { entraAdapter.tema("$UUID") } returns setOf(TEMA)

                mockMvc.perform(get("/api/v1/tema"))
                    .andExpect(status().isOk)
                    .andDo(document("tema/obo", preprocessResponse(prettyPrint())))
            }
        }
    }

    Given("medlemmer for enhet-endepunkt") {
        When("enheten har medlemmer") {
            Then("skal dokumentere responsen") {
                every { oid.gruppeOid(ENHET.enhetnummer.gruppeNavn) } returns UUID
                every { entraAdapter.gruppeMedlemmer("$UUID") } returns setOf(ansatt)

                mockMvc.perform(get("/api/v1/enhet/${ENHET.enhetnummer.verdi}"))
                    .andExpect(status().isOk)
                    .andDo(document("enhet/medlemmer", preprocessResponse(prettyPrint())))
            }
        }
    }

    Given("ansatt tident-endepunkt") {
        When("ansatt finnes") {
            Then("skal dokumentere responsen") {
                every { entraAdapter.utvidetAnsattTident(TIDENT.verdi) } returns ANSATT_RESPONS
                every { norg.navnFor(ENHETNUMMER) } returns ENHETSNAVN

                mockMvc.perform(get("/api/v1/ansatt/tident/${TIDENT.verdi}"))
                    .andExpect(status().isOk)
                    .andDo(document("ansatt/tident", preprocessResponse(prettyPrint())))
            }
        }
    }

    Given("tilganger-endepunkt") {
        When("ansatt har grupper") {
            Then("skal dokumentere responsen") {
                every { oid.ansattOid(ANSATTID) } returns UUID
                every { entraAdapter.ansatteGrupper("$UUID") } returns setOf(GRUPPE)

                mockMvc.perform(get("/api/v1/ansatt/tilganger/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andDo(document("ansatt/tilganger", preprocessResponse(prettyPrint())))
            }
        }
    }

    Given("gruppe medlemmer-endepunkt") {
        When("gruppe finnes") {
            Then("skal dokumentere responsen") {
                val gruppeNavn = "0000-GA-ENHET_1234"
                every { oid.gruppeOid(gruppeNavn) } returns UUID
                every { entraAdapter.gruppeMedlemmer("$UUID") } returns setOf(ansatt)

                mockMvc.perform(get("/api/v1/gruppe/medlemmer").param("gruppeNavn", gruppeNavn))
                    .andExpect(status().isOk)
                    .andDo(document("gruppe/medlemmer", preprocessResponse(prettyPrint())))
            }
        }
    }

    Given("NorgTjeneste ved henting av flere enheter") {
        When("ansatt har flere enheter") {
            Then("skal kalle Norg for hver enhet") {
                val uuid = randomUUID()
                val enhet2 = Enhetnummer("5678")
                every { oid.ansattOid(ANSATTID) } returns uuid
                every { entraAdapter.enheter("$uuid") } returns setOf(ENHETNUMMER, enhet2)
                every { norg.navnFor(ENHETNUMMER) } returns ENHETSNAVN
                every { norg.navnFor(enhet2) } returns "NAV Oslo"

                val respons = mockMvc.perform(get("/api/v1/enhet/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().response.contentAsString

                val enheter = jsonMapper.readValue<Set<Enhet>>(respons)
                enheter.size shouldBe 2

                verify(exactly = 1) { norg.navnFor(ENHETNUMMER) }
                verify(exactly = 1) { norg.navnFor(enhet2) }
            }
        }
    }

    Given("NorgTjeneste feiler ved enhet-oppslag") {
        When("Norg kaster NotFoundRestException for en enhet") {
            Then("skal feile med feilmelding") {
                val uuid = randomUUID()
                every { oid.ansattOid(ANSATTID) } returns uuid
                every { entraAdapter.enheter("$uuid") } returns setOf(ENHETNUMMER)
                every { norg.navnFor(ENHETNUMMER) } throws NotFoundRestException(
                    URI.create("http://norg2.org/norg2/api/v1/enhet/${ENHETNUMMER.verdi}"), "Not Found"
                )

                mockMvc.perform(get("/api/v1/enhet/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isNotFound)
            }
        }
    }

    Given("NorgTjeneste brukes ved henting av utvidet ansatt") {
        When("ansatt finnes med streetAddress (enhetsnummer)") {
            Then("skal berike med enhetsnavn fra Norg") {
                every { entraAdapter.utvidetAnsatt(ANSATTID.verdi) } returns ANSATT_RESPONS
                every { norg.navnFor(ENHETNUMMER) } returns ENHETSNAVN

                val respons = mockMvc.perform(get("/api/v1/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andDo(document("ansatt/utvidet", preprocessResponse(prettyPrint())))
                    .andReturn().response.contentAsString

                respons shouldContain ENHETSNAVN
                respons shouldContain "4242"

                verify(exactly = 1) { norg.navnFor(ENHETNUMMER) }
            }
        }

        When("Norg feiler ved henting av enhetsnavn for ansatt") {
            Then("skal propagere feilen") {
                every { entraAdapter.utvidetAnsatt(ANSATTID.verdi) } returns ANSATT_RESPONS
                every { norg.navnFor(any()) } throws NotFoundRestException(
                    URI.create("http://norg2.org/norg2/api/v1/enhet/${ANSATT_RESPONS.streetAddress}"), "Not Found"
                )

                mockMvc.perform(get("/api/v1/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isNotFound)
            }
        }
    }

    Given("Enhetnummer") {
        Then("gruppeNavn og verdi uten prefix") {
            enhetnr.gruppeNavn shouldBe "${ENHET_PREFIX}$nummer"
            enhetnr.verdi shouldBe nummer
        }
        Then("gruppeNavn og verdi med prefix") {
            enhetnr1.gruppeNavn shouldBe "${ENHET_PREFIX}$nummer"
            enhetnr1.verdi shouldBe nummer
        }
    }

    Given("Tema") {
        Then("gruppeNavn og verdi uten prefix") {
            TEMA.gruppeNavn shouldBe "${Tema.TEMA_PREFIX}$AAP"
            TEMA.verdi shouldBe AAP
        }
        Then("gruppeNavn og verdi med prefix") {
            val tema1 = Tema("${Tema.TEMA_PREFIX}$AAP")
            tema1.gruppeNavn shouldBe "${Tema.TEMA_PREFIX}$AAP"
            tema1.verdi shouldBe AAP
        }
    }
}) {
    companion object {
        const val AAP = "AAP"
        val ANSATTID = AnsattId("A123456")
        val TIDENT = TIdent("AAA1234")
        val UUID = randomUUID()
        val TEMA = Tema(AAP)
        val ansatt = Ansatt(AnsattId("E123456"), "Ola Nordmann", "Ola", "Nordmann")
        val ENHET = Enhet(Enhetnummer("1234"), "Enhet Navn")
        val ENHETNUMMER = Enhetnummer("4242")
        const val ENHETSNAVN = "NAV Testkontor"
        const val nummer = "1234"
        val enhetnr = Enhetnummer(nummer)
        val enhetnr1 = Enhetnummer("${ENHET_PREFIX}$nummer")
        val GRUPPE = EntraGruppe("0000-GA-ENHET_1234")
        val ANSATT_RESPONS = AnsattRespons(
            randomUUID(),
            "A123456",
            "Ola Nordmann",
            "Ola",
            "Nordmann",
            "AAA1234",
            "ola@nav.no",
            "4242"
        )
    }
}
