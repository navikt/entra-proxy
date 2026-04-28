package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.verify
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.OAuth2ClientRequestInterceptor
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheClient
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenTypeTellendeRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenTypeTeller
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.DevEntraController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.util.UUID.randomUUID

@WebMvcTest(controllers = [DevEntraController::class])
@Import(TestConfig::class)
@ApplyExtension(SpringExtension::class)
class TilgangTester(
    @param:Autowired
    private val jsonMapper: JsonMapper,
    @param:Autowired
    private val mockMvc: MockMvc,
    @MockkBean(relaxed = true)
    private val token: Token,
    @MockkBean(relaxed = true)
    private val oAuth2ClientRequestInterceptor: OAuth2ClientRequestInterceptor,
    @MockkBean(relaxed = true)
    private val clientConfigurationProperties: ClientConfigurationProperties,
    @MockkBean
    private val entraAdapter: EntraRestClientAdapter,
    @MockkBean
    private val oid: EntraOidTjeneste,
    @MockkBean(relaxed = true)
    private val norg: NorgTjeneste,
    @MockkBean(relaxed = true)
    private val cache: CacheClient,
    @MockkBean(relaxed = true)
    private val teller: TokenTypeTellendeRequestInterceptor, ) : BehaviorSpec({

    beforeSpec {
        every { token.systemAndNs } returns "test:ns"
        every { token.systemNavn } returns "Test"
    }

    Given("tema-endepunkt") {
        When("det finnes medlemmer") {
            Then("skal responsen inneholde forventet ansatt") {
                every { oid.gruppeOid(TEMA.gruppeNavn) } returns UUID
                every { entraAdapter.gruppeMedlemmer("$UUID") } returns setOf(ansatt)
                val respons = mockMvc.perform(get("/dev/tema/$AAP"))
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
                val respons = mockMvc.perform(get("/dev/enhet/ansatt/${ANSATTID.verdi}"))
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

@Configuration(proxyBeanMethods = true)
class TestConfig {
    @Bean
    fun meterRegistry() = SimpleMeterRegistry()

    @Bean
    fun cacheManager() = NoOpCacheManager()

    @Bean
    fun entra(adapter: EntraRestClientAdapter, norg: NorgTjeneste, oid: EntraOidTjeneste, cache: CacheClient) =
        EntraTjeneste(adapter, norg, oid, cache)

    @Bean
    fun teller(reg: MeterRegistry) =
        TokenTypeTellendeRequestInterceptor(TokenTypeTeller(reg, Token(SpringTokenValidationContextHolder())))
}