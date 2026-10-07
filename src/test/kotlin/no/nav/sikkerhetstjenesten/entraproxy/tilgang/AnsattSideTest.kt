package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.BehaviorSpec
import no.nav.sikkerhetstjenesten.entraproxy.felles.nom.NomTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.SecurityTestSupport.setProperties
import org.hamcrest.Matchers.containsString
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * ansatt.html skal kun v\u00e6re tilgjengelig n\u00e5r applikasjonen ikke kj\u00f8rer i prod.
 * I motsetning til EntraControllerTest settes IKKE NAIS_CLUSTER_NAME til prod-gcp her,
 * slik at AnsattSideController (@ConditionalOnNotProd) blir registrert.
 */
@SpringBootTest(classes = [SecurityTestApplication::class])
@AutoConfigureMockMvc
class AnsattSideTest(private val mockMvc: MockMvc) : BehaviorSpec() {

    @MockkBean
    private lateinit var entraTjeneste: EntraTjeneste

    @MockkBean
    private lateinit var oidTjeneste: EntraOidTjeneste

    @MockkBean
    private lateinit var norgTjeneste: NorgTjeneste

    @MockkBean
    private lateinit var nomTjeneste: NomTjeneste

    init {
        Given("ansatt.html i et milj\u00f8 som ikke er prod") {
            When("siden hentes") {
                Then("vises siden med lenker til gruppe- og tilgangsdata") {
                    mockMvc.perform(get("/ansatt.html"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(TEXT_HTML))
                        .andExpect(content().string(containsString("/api/v1/gruppe/antall?")))
                        .andExpect(content().string(containsString("Antall medlemmer")))
                        .andExpect(content().string(containsString("/api/v1/ansatt/tilganger/")))
                        .andExpect(content().string(containsString("Gruppemedlemskap")))
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerMockOAuth2Server(registry: DynamicPropertyRegistry) {
            registry.setProperties()
        }
    }
}
