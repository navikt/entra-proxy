package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.ValkeyCacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AuthContext
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController.Companion.API_V1
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.util.UUID.randomUUID

private inline fun <reified T> MvcResult.bodyAs(mapper: JsonMapper): T =
    mapper.readValue(response.contentAsString)

class EntraTjenesteTest : BehaviorSpec({

    val token: AuthContext = mockk()
    val entraAdapter: EntraRestClientAdapter = mockk()
    val oid: EntraOidTjeneste = mockk()
    val norg: NorgTjeneste = mockk()
    val cache: ValkeyCacheOperations = mockk(relaxed = true)
    val entra = EntraTjeneste(entraAdapter, norg, oid, cache)
    val controller = EntraController(entra, oid)
    val mockMvc: MockMvc = standaloneSetup(controller).build()
    val mapper = JsonMapper.builder().findAndAddModules().build()

    beforeSpec {
        every { token.systemAndNs } returns "test:ns"
        every { token.systemNavn } returns "Test"
    }

    Given("tema-endepunkt") {
        When("det finnes medlemmer") {
            Then("skal responsen inneholde forventet ansatt") {
                every { oid.gruppeOid(TEMA.gruppeNavn) } returns UUID
                every { entraAdapter.gruppeMedlemmer("$UUID") } returns setOf(ansatt)
                mockMvc.perform(get("${API_V1}/tema/${AAP}"))
                    .andExpect(status().isOk)
                    .andReturn().bodyAs<Set<Ansatt>>(mapper).single() shouldBe ansatt
            }
        }
    }

    Given("enhet-endepunkt") {
        When("adapter feiler første gang og lykkes etter refreshOid") {
            Then("skal responsen inneholde forventet enhet") {
                every { oid.ansattOid(ANSATTID) } returns UUID
                every { entraAdapter.enheterForAnsatt("$UUID") } throws
                    NotFoundRestException(URI.create(""), "ikke funnet") andThen setOf(ENHET.enhetnummer)
                every { norg.navnFor(ENHET.enhetnummer) } returns ENHET.navn
                mockMvc.perform(get("${API_V1}/enhet/ansatt/${ANSATTID.verdi}"))
                    .andExpect(status().isOk)
                    .andReturn().bodyAs<Set<Enhet>>(mapper).single() shouldBe ENHET
                verify(exactly = 2)  { entraAdapter.enheterForAnsatt("$UUID") }
            }
        }
    }
}) {
    private companion object {
        private const val AAP = "AAP"
        private val ANSATTID = AnsattId("A123456")
        private val UUID = randomUUID()
        private val TEMA = Tema(AAP)
        private val ansatt = Ansatt(AnsattId("E123456"), "Ola Nordmann", "Ola", "Nordmann")
        private val ENHET = Enhet(Enhetnummer("1234"), "Enhet Navn")
    }
}
