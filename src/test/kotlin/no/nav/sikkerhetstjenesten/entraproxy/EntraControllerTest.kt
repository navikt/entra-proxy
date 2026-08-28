package no.nav.sikkerhetstjenesten.entraproxy

import com.nimbusds.oauth2.sdk.token.AccessTokenType.BEARER
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.APP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.IDTYP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.CCF
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.OBO
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.graph.UtvidetAnsatt
import no.nav.sikkerhetstjenesten.entraproxy.security.FEIL_AUDIENCE
import no.nav.sikkerhetstjenesten.entraproxy.security.INVALID_AUDIENCE
import no.nav.sikkerhetstjenesten.entraproxy.security.MANGLER_BEARER_TOKEN
import no.nav.sikkerhetstjenesten.entraproxy.security.SecurityTestApplication
import no.nav.sikkerhetstjenesten.entraproxy.security.TEST_AUDIENCE
import no.nav.sikkerhetstjenesten.entraproxy.security.TEST_ANSATT_ID
import no.nav.sikkerhetstjenesten.entraproxy.security.TYPE_URI
import no.nav.sikkerhetstjenesten.entraproxy.security.jwt
import no.nav.sikkerhetstjenesten.entraproxy.security.setProperties
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.PROD_BASE_PATH
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.HttpStatus.Series.CLIENT_ERROR
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatus
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@SpringBootTest(classes = [SecurityTestApplication::class])
@AutoConfigureMockMvc
@ApplyExtension(SpringExtension::class)
class EntraControllerTest(private val mapper: JsonMapper) : BehaviorSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var entraTjeneste: EntraTjeneste

    @MockkBean
    private lateinit var oidTjeneste: EntraOidTjeneste

    @MockkBean
    private lateinit var token: Token

    init {
        beforeEach {
            every { token.type } returns CCF
            every { token.ansattId } returns TEST_ANSATT_ID
            every { token.oid } returns REQUEST_OID
        }

        Given("auth validation for all controller endpoints") {
            endpointRequests.forEach { (name, request) ->
                Then("$name returns 401 when authorization header is missing") {
                    mockMvc.perform(request())
                        .andExpect(status().isUnauthorized)
                        .andExpect(problemDetail(UNAUTHORIZED))

                }

                Then("$name returns 401 when token audience is wrong") {
                    mockMvc.perform(request().withBearer(invalidAudienceToken()))
                        .andExpect(status().isUnauthorized)
                        .andExpect(problemDetail(UNAUTHORIZED, FEIL_AUDIENCE))
                }
            }
        }

        Given("token type validation on protected endpoints") {
            Then("CCF endpoint returns 403 for OBO token type") {
                every { token.type } returns OBO

                mockMvc.perform(get("$PROD_BASE_PATH/enhet/ansatt/${NAV_IDENT.verdi}").withBearer(ccfToken()))
                    .andExpect(status().isForbidden)
                    .andExpect(problemDetail(FORBIDDEN))

            }

            Then("OBO endpoint returns 403 for CCF token type") {
                every { token.type } returns CCF

                mockMvc.perform(get("$PROD_BASE_PATH/enhet").withBearer(oboToken()))
                    .andExpect(status().isForbidden)
            }
        }

        Given("successful requests for all endpoints") {
            Then("returns enheter for CCF endpoint") {
                every { oidTjeneste.ansattOid(NAV_IDENT) } returns REQUEST_OID
                every { entraTjeneste.enheter(NAV_IDENT, REQUEST_OID) } returns setOf(ENHET)

                mockMvc.perform(get("$PROD_BASE_PATH/enhet/ansatt/${NAV_IDENT.verdi}").withBearer(ccfToken()))
                    .andExpect(status().isOk)

                verify(exactly = 1) { oidTjeneste.ansattOid(NAV_IDENT) }
                verify(exactly = 1) { entraTjeneste.enheter(NAV_IDENT, REQUEST_OID) }
            }

            Then("returns enheter for OBO endpoint") {
                every { token.type } returns OBO
                every { entraTjeneste.enheter(TEST_ANSATT_ID, REQUEST_OID) } returns setOf(ENHET)

                mockMvc.perform(get("$PROD_BASE_PATH/enhet").withBearer(oboToken()))
                    .andExpect(status().isOk)

                verify(exactly = 1) { entraTjeneste.enheter(TEST_ANSATT_ID, REQUEST_OID) }
            }

            Then("returns tema for CCF endpoint") {
                every { oidTjeneste.ansattOid(NAV_IDENT) } returns REQUEST_OID
                every { entraTjeneste.tema(NAV_IDENT, REQUEST_OID) } returns setOf(TEMA)

                mockMvc.perform(get("$PROD_BASE_PATH/tema/ansatt/${NAV_IDENT.verdi}").withBearer(ccfToken()))
                    .andExpect(status().isOk)

                verify(exactly = 1) { oidTjeneste.ansattOid(NAV_IDENT) }
                verify(exactly = 1) { entraTjeneste.tema(NAV_IDENT, REQUEST_OID) }
            }

            Then("returns tema for OBO endpoint") {
                every { token.type } returns OBO
                every { entraTjeneste.tema(TEST_ANSATT_ID, REQUEST_OID) } returns setOf(TEMA)

                mockMvc.perform(get("$PROD_BASE_PATH/tema").withBearer(oboToken()))
                    .andExpect(status().isOk)

                verify(exactly = 1) { entraTjeneste.tema(TEST_ANSATT_ID, REQUEST_OID) }
            }

            Then("returns enhetens medlemmer") {
                every { oidTjeneste.gruppeOid(ENHET_NUMMER.gruppeNavn) } returns GROUP_OID
                every { entraTjeneste.medlemmer(GROUP_OID) } returns setOf(ANSATT)

                mockMvc.perform(get("$PROD_BASE_PATH/enhet/${ENHET_NUMMER.verdi}").withBearer(ccfToken()))
                    .andExpect(status().isOk)

                verify(exactly = 1) { oidTjeneste.gruppeOid(ENHET_NUMMER.gruppeNavn) }
                verify(exactly = 1) { entraTjeneste.medlemmer(GROUP_OID) }
            }

            Then("returns temaets medlemmer") {
                every { oidTjeneste.gruppeOid(TEMA.gruppeNavn) } returns GROUP_OID
                every { entraTjeneste.medlemmer(GROUP_OID) } returns setOf(ANSATT)

                mockMvc.perform(get("$PROD_BASE_PATH/tema/${TEMA.verdi}").withBearer(ccfToken()))
                    .andExpect(status().isOk)

                verify(exactly = 1) { oidTjeneste.gruppeOid(TEMA.gruppeNavn) }
                verify(exactly = 1) { entraTjeneste.medlemmer(GROUP_OID) }
            }

            Then("returns employee by navIdent") {
                every { entraTjeneste.utvidetAnsatt(NAV_IDENT) } returns UTVIDET_ANSATT

                mockMvc.perform(get("$PROD_BASE_PATH/ansatt/${NAV_IDENT.verdi}").withBearer(ccfToken()))
                    .andExpect(status().isOk)

                verify(exactly = 1) { entraTjeneste.utvidetAnsatt(NAV_IDENT) }
            }

            Then("returns employee by tIdent") {
                every { entraTjeneste.utvidetAnsatt(T_IDENT) } returns UTVIDET_ANSATT

                mockMvc.perform(get("$PROD_BASE_PATH/ansatt/tident/${T_IDENT.verdi}").withBearer(ccfToken()))
                    .andExpect(status().isOk)

                verify(exactly = 1) { entraTjeneste.utvidetAnsatt(T_IDENT) }
            }

            Then("returns accesses for employee") {
                every { oidTjeneste.ansattOid(NAV_IDENT) } returns REQUEST_OID
                every { entraTjeneste.grupperForAnsatt(NAV_IDENT, REQUEST_OID) } returns setOf(EntraGruppe("gruppe"))

                mockMvc.perform(get("$PROD_BASE_PATH/ansatt/tilganger/${NAV_IDENT.verdi}").withBearer(ccfToken()))
                    .andExpect(status().isOk)

                verify(exactly = 1) { oidTjeneste.ansattOid(NAV_IDENT) }
                verify(exactly = 1) { entraTjeneste.grupperForAnsatt(NAV_IDENT, REQUEST_OID) }
            }

            Then("returns members for gruppe endpoint") {
                every { oidTjeneste.gruppeOid(GRUPPE_NAVN) } returns GROUP_OID
                every { entraTjeneste.medlemmer(GROUP_OID) } returns setOf(ANSATT)

                mockMvc.perform(
                    get("$PROD_BASE_PATH/gruppe/medlemmer")
                        .param("gruppeNavn", GRUPPE_NAVN)
                        .withBearer(ccfToken())
                ).andExpect(status().isOk)

                verify(exactly = 1) { oidTjeneste.gruppeOid(GRUPPE_NAVN) }
                verify(exactly = 1) { entraTjeneste.medlemmer(GROUP_OID) }
            }
        }
    }

    private fun MockHttpServletRequestBuilder.withBearer(token: String) =
        header(AUTHORIZATION, "$BEARER $token")

    private fun ccfToken() = jwt(TEST_AUDIENCE, TEST_ANSATT_ID, mapOf(IDTYP to APP))

    private fun oboToken() = jwt(TEST_AUDIENCE, TEST_ANSATT_ID)

    private fun invalidAudienceToken() = jwt(INVALID_AUDIENCE, TEST_ANSATT_ID, mapOf(IDTYP to APP))

    private fun problemDetail(status: HttpStatus, detail: String? = null): ResultMatcher =
        ResultMatcher { result ->
            val actual = mapper.readValue(result.response.contentAsByteArray, ProblemDetail::class.java)
            actual.status shouldBe status.value()
            actual.title shouldBe status.value().toString()
            detail?.let { it shouldBe actual.detail }
            actual.type shouldBe TYPE_URI
        }

    companion object {
        private val NAV_IDENT = AnsattId("Z123456")
        private val T_IDENT = TIdent("ABC1234")
        private val REQUEST_OID = UUID.fromString("00000000-0000-0000-0000-000000000042")
        private val GROUP_OID = UUID.fromString("00000000-0000-0000-0000-000000000123")
        private val ENHET_NUMMER = Enhet.Enhetnummer("4242")
        private val ENHET = Enhet(ENHET_NUMMER, "NAV Testkontor")
        private val TEMA = Tema("AAP")
        private const val GRUPPE_NAVN = "0000-GA-TEMA_AAP"
        private val ANSATT = Ansatt(
            NAV_IDENT,
            "Ola Nordmann",
            "Ola",
            "Nordmann"
        )
        private val UTVIDET_ANSATT = UtvidetAnsatt(
            NAV_IDENT,
            "Ola Nordmann",
            "Ola",
            "Nordmann",
            T_IDENT,
            "ola.nordmann@nav.no",
            ENHET
        )
        private val endpointRequests: List<Pair<String, () -> MockHttpServletRequestBuilder>> = listOf(
            "GET /enhet/ansatt/{navIdent}" to { get("$PROD_BASE_PATH/enhet/ansatt/${NAV_IDENT.verdi}") },
            "GET /enhet" to { get("$PROD_BASE_PATH/enhet") },
            "GET /tema/ansatt/{navIdent}" to { get("$PROD_BASE_PATH/tema/ansatt/${NAV_IDENT.verdi}") },
            "GET /tema" to { get("$PROD_BASE_PATH/tema") },
            "GET /enhet/{enhetsnummer}" to { get("$PROD_BASE_PATH/enhet/${ENHET_NUMMER.verdi}") },
            "GET /tema/{tema}" to { get("$PROD_BASE_PATH/tema/${TEMA.verdi}") },
            "GET /ansatt/{navIdent}" to { get("$PROD_BASE_PATH/ansatt/${NAV_IDENT.verdi}") },
            "GET /ansatt/tident/{tIdent}" to { get("$PROD_BASE_PATH/ansatt/tident/${T_IDENT.verdi}") },
            "GET /ansatt/tilganger/{navIdent}" to { get("$PROD_BASE_PATH/ansatt/tilganger/${NAV_IDENT.verdi}") },
            "GET /gruppe/medlemmer" to { get("$PROD_BASE_PATH/gruppe/medlemmer").param("gruppeNavn", GRUPPE_NAVN) },
        )



        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.setProperties()
        }
    }
}
