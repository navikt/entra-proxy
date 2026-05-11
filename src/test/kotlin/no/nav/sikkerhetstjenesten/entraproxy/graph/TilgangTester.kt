package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheClient
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.util.UUID.randomUUID

class TilgangTester : BehaviorSpec({

    val token: Token = mockk(relaxed = true)
    val entraAdapter: EntraRestClientAdapter = mockk()
    val oid: EntraOidTjeneste = mockk()
    val norg: NorgTjeneste = mockk(relaxed = true)
    val cache: CacheClient = mockk(relaxed = true)
    val entra = EntraTjeneste(entraAdapter, norg, oid, cache)
    val controller = EntraController(entra, oid, token)
    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(controller).build()
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

    Given("tema-endepunkt") {
        When("det finnes medlemmer") {
            Then("skal responsen inneholde forventet ansatt") {
                every { oid.gruppeOid(TEMA.gruppeNavn) } returns UUID
                every { entraAdapter.gruppeMedlemmer("$UUID") } returns setOf(ansatt)
                val respons = mockMvc.perform(get("/api/v1/tema/$AAP"))
                    .andExpect(status().isOk)
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
                    .andReturn().response.contentAsString
                jsonMapper.readValue<Set<Enhet>>(respons).single() shouldBe ENHET
                verify(exactly = 2)  { entraAdapter.enheter("$UUID") }
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
        val UUID = randomUUID()
        val TEMA = Tema(AAP)
        val ansatt = Ansatt(AnsattId("E123456"), "Ola Nordmann", "Ola", "Nordmann")
        val ENHET = Enhet(Enhetnummer("1234"), "Enhet Navn")
        const val nummer = "1234"
        val enhetnr = Enhetnummer(nummer)
        val enhetnr1 = Enhetnummer("${ENHET_PREFIX}$nummer")
    }
}
