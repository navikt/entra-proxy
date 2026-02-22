package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.junit5.MockKExtension
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.OAuth2ClientRequestInterceptor
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenTypeTellendeRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenTypeTeller
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.DevEntraController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
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
import java.util.UUID.*
import kotlin.test.BeforeTest
import kotlin.test.Test

@WebMvcTest(controllers = [DevEntraController::class])
@Import(TestConfig::class)
@ExtendWith(MockKExtension::class)
class TilgangTester {
    @Autowired
    private lateinit var jsonMapper: JsonMapper
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var token: Token
    @MockkBean
    private lateinit var oAuth2ClientRequestInterceptor: OAuth2ClientRequestInterceptor

    @MockkBean
    private lateinit var clientConfigurationProperties: ClientConfigurationProperties

    @MockkBean
    private lateinit var entra: EntraTjeneste

    @MockkBean
    private lateinit var oid: EntraOidTjeneste

    @MockkBean
    private lateinit var norg: NorgTjeneste

    @MockkBean
    private lateinit var teller : TokenTypeTellendeRequestInterceptor



    @BeforeTest
    fun setup() {
        every { token.systemAndNs } returns "test:ns"
        every { token.systemNavn } returns "Test"
    }

    @Test
    fun temaer() {
        every { oid.gruppeOid( TEMA.gruppeNavn) } returns UUID
        every { entra.medlemmer(UUID) } returns setOf(ansatt)
        val respons = mockMvc.perform((get("/dev/tema/$AAP")))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        assertThat(jsonMapper.readValue<Set<Ansatt>>(respons).single()).isEqualTo(ansatt)
    }

    @Test
    fun enheter() {
        every { oid.ansattOid(ANSATTID) } returns UUID
        every { entra.enheter(ANSATTID,UUID) } returns setOf(ENHET)
        val respons = mockMvc.perform((get("/dev/enhet/ansatt/${ANSATTID.verdi}")))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        assertThat(jsonMapper.readValue<Set<Enhet>>(respons).single()).isEqualTo(ENHET)
    }

    @Test
    fun enhetUtenPrefix() {
        assertThat(enhetnr.gruppeNavn).isEqualTo("${ENHET_PREFIX}$nummer")
        assertThat(enhetnr.verdi).isEqualTo(nummer)
    }
    @Test
    fun enhetMedPrefix() {
        assertThat(enhetnr1.gruppeNavn).isEqualTo("${ENHET_PREFIX}$nummer")
        assertThat(enhetnr1.verdi).isEqualTo(nummer)
    }

    @Test
    fun temaUtenPrefix() {
        assertThat(TEMA.gruppeNavn).isEqualTo("${Tema.Companion.TEMA_PREFIX}$AAP")
        assertThat(TEMA.verdi).isEqualTo(AAP)
    }
    @Test
    fun temaMedPrefix() {
        val tema1 = Tema("${Tema.TEMA_PREFIX}$AAP")
        assertThat(tema1.gruppeNavn).isEqualTo("${Tema.TEMA_PREFIX}$AAP")
        assertThat(tema1.verdi).isEqualTo(AAP)
    }


    companion object {
        const val AAP = "AAP"
        val ANSATTID = AnsattId("A123456")
        val UUID = randomUUID()
        val TEMA = Tema(AAP)
        private val ansatt = Ansatt(
            AnsattId("E123456"),
            "Ola Nordmann",
            "Ola",
            "Nordmann")

        private val ENHET = Enhet(
            Enhetnummer("1234"),
            "Enhet Navn"
        )
        private const val nummer = "1234"
        val enhetnr = Enhetnummer(nummer)
        val enhetnr1 = Enhetnummer("${ENHET_PREFIX}$nummer")
    }
}

@Configuration(proxyBeanMethods = false)
class TestConfig {
    @Bean
    fun meterRegistry() = SimpleMeterRegistry()

    @Bean
    fun cacheManager() = NoOpCacheManager()

    @Bean
    fun teller() = TokenTypeTellendeRequestInterceptor(TokenTypeTeller(meterRegistry(),
        Token(SpringTokenValidationContextHolder())))
}