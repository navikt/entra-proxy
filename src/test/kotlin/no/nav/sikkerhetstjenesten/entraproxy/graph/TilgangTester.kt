package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.junit5.MockKExtension
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.OAuth2ClientRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheBeanConfig.Companion.MAPPER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenTypeTellendeRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.DevEntraController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID.*
import kotlin.test.Test


@WebMvcTest(controllers = [DevEntraController::class])
@Import(TestConfig::class)
@ExtendWith(MockKExtension::class)
class TilgangTester(@Autowired private val token: Token) {


    @Autowired
    private lateinit var jsonMapper: JsonMapper
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var t: Token
    @MockkBean
    private lateinit var i: OAuth2ClientRequestInterceptor

    @MockkBean
    private lateinit var m: CacheManager

    @MockkBean
    private lateinit var c: ClientConfigurationProperties

    @MockkBean
    private lateinit var entra: EntraTjeneste

    @MockkBean
    private lateinit var oid: EntraOidTjeneste

    @MockkBean
    private lateinit var norg: NorgTjeneste

    @MockkBean
    private lateinit var teller : TokenTypeTellendeRequestInterceptor

    private val nummer = "1234"
    private val aap = "AAP"
    val enhet = Enhetnummer(nummer)
    val enhet1 = Enhetnummer("${Enhet.ENHET_PREFIX}$nummer")
    val ansatt = Ansatt(
        AnsattId("E123456"),
        "Ola Nordmann",
        "Ola",
        "Nordmann")

    val uuid = randomUUID()
    val tema = Tema(aap)
    val tema1 = Tema("${Tema.TEMA_PREFIX}$aap")

    @Test
    fun entra() {
        every { token.erObo } returns true
        every { token.systemAndNs } returns "test:ns"
        every { token.systemNavn } returns "Test"
        every { oid.gruppeOid( tema.gruppeNavn) } returns uuid
        every { entra.medlemmer(uuid) } returns setOf(ansatt)
        val respons = mockMvc.perform((get("/dev/tema/AAP")))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString;
        val res = jsonMapper.readValue<Set<Ansatt>>(respons)
        assertThat(res.single()).isEqualTo(ansatt)
    }

    @Test
    fun enhetUtenPrefix() {
        assertThat(enhet.gruppeNavn).isEqualTo("${Enhet.ENHET_PREFIX}$nummer")
        assertThat(enhet.verdi).isEqualTo(nummer)
    }
    @Test
    fun enhetMedPrefix() {
        assertThat(enhet1.gruppeNavn).isEqualTo("${Enhet.ENHET_PREFIX}$nummer")
        assertThat(enhet1.verdi).isEqualTo(nummer)
    }

    @Test
    fun serDeserEnhet() {
        assertThat(MAPPER.readValue<Enhetnummer>(MAPPER.writeValueAsString(
            enhet)).verdi).isEqualTo(nummer)
        assertThat(MAPPER.readValue<Enhetnummer>(MAPPER.writeValueAsString(
            enhet1)).verdi).isEqualTo(nummer)
    }
    @Test
    fun temaUtenPrefix() {
        assertThat(tema.gruppeNavn).isEqualTo("${Tema.Companion.TEMA_PREFIX}$aap")
        assertThat(tema.verdi).isEqualTo(aap)
    }
    @Test
    fun temaMedPrefix() {
        assertThat(tema1.gruppeNavn).isEqualTo("${Tema.TEMA_PREFIX}$aap")
        assertThat(tema1.verdi).isEqualTo(aap)
    }

    @Test
    fun serDeserTema() {
        assertThat(MAPPER.readValue<Tema>(MAPPER.writeValueAsString(
            tema)).verdi).isEqualTo(aap)
        assertThat(MAPPER.readValue<Tema>(MAPPER.writeValueAsString(
            tema1)).verdi).isEqualTo(aap)
    }
}

@Configuration
class TestConfig {
    @Bean
    fun meterRegistry() = SimpleMeterRegistry()
}