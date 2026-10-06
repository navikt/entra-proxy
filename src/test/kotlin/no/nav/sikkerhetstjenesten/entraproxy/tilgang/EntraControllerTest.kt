package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.PROD_GCP
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.graph.UtvidetAnsatt
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController.Companion.API_V1
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.SecurityTestSupport.TEST_ANSATT_ID
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.SecurityTestSupport.TEST_ENHET
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.SecurityTestSupport.ccJwt
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.SecurityTestSupport.oboJwt
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.SecurityTestSupport.setProperties
import org.hamcrest.Matchers.containsString
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import java.util.UUID

@SpringBootTest(classes = [SecurityTestApplication::class])
@AutoConfigureMockMvc
class EntraControllerTest(private val mockMvc: MockMvc) : BehaviorSpec() {


    @MockkBean
    private lateinit var entraTjeneste: EntraTjeneste

    @MockkBean
    private lateinit var oidTjeneste: EntraOidTjeneste

    @MockkBean
    private lateinit var norgTjeneste: NorgTjeneste

    init {

        beforeEach {
            every { oidTjeneste.ansattOid(TEST_ANSATT_ID) } returns UUID.randomUUID()
            every { oidTjeneste.gruppeOid(any()) } returns UUID.randomUUID()
            every { entraTjeneste.enheter(any(), any()) } returns sortedSetOf(TEST_ENHET)
            every { entraTjeneste.tema(any(), any()) } returns sortedSetOf(Tema("AAP"))
            every { entraTjeneste.medlemmerIGruppe(any(), any()) } returns sortedSetOf(Ansatt(TEST_ANSATT_ID, "Test Ansatt", "Test", "Ansatt"))
            every { entraTjeneste.grupperForAnsatt(any(), any()) } returns sortedSetOf(EntraGruppe("test-rolle"))
            every { entraTjeneste.utvidetAnsatt(TEST_ANSATT_ID) } returns UtvidetAnsatt(
                TEST_ANSATT_ID,
                "Test Ansatt",
                "Test",
                "Ansatt",
                TIdent("A123456"),
                "test@nav.no",
                TEST_ENHET,
            )
            every { entraTjeneste.utvidetAnsatt(TIdent("A123456")) } returns UtvidetAnsatt(
                TEST_ANSATT_ID,
                "Test Ansatt",
                "Test",
                "Ansatt",
                TIdent("A123456"),
                "test@nav.no",
                TEST_ENHET,
            )
        }

        xGiven("den statiske utforskersiden") {
            When("request mangler bearer-token") {
                Then("viser linker til ansatt og gruppemedlemmer") {
                    mockMvc.perform(get("/utforsker.html"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith("text/html"))
                        .andExpect(content().string(containsString("href=\"/ansatt.html\"")))
                        .andExpect(content().string(containsString("href=\"/grupper.html\"")))
                }
            }
        }

        Given("beskyttet endepunkt ${API_V1}/enhet") {
            When("request mangler bearer-token") {
                Then("returnerer 401") {
                    mockMvc.perform(get("${API_V1}/enhet"))
                        .andExpect {
                            status().isUnauthorized
                        }
                }
            }

            When("request har gyldig OBO-token") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/enhet").header(AUTHORIZATION, oboJwt()))
                        .andExpect {
                            status().isOk
                        }
                }
            }

            When("request har CCF-token") {
                Then("returnerer 403") {
                    mockMvc.perform(get("${API_V1}/enhet").header(AUTHORIZATION, ccJwt()))
                        .andExpect {
                            status().isForbidden
                        }
                }
            }
        }

        Given("beskyttet endepunkt ${API_V1}/enhet/ansatt/{navIdent}") {
            When("request har OBO-token") {
                Then("returnerer 403") {
                    mockMvc.perform(get("${API_V1}/enhet/ansatt/${TEST_ANSATT_ID}").header(AUTHORIZATION, oboJwt()))
                        .andExpect {
                            status().isForbidden
                        }
                }
            }

            When("request har gyldig CCF-token") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/enhet/ansatt/${TEST_ANSATT_ID}").header(AUTHORIZATION, ccJwt()))
                        .andExpect {
                            status().isOk
                        }
                }
            }
        }

        Given("beskyttet endepunkt ${API_V1}/tema") {
            When("request har gyldig OBO-token") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/tema").header(AUTHORIZATION, oboJwt()))
                        .andExpect {
                            status().isOk
                        }
                }
            }

            When("request har CCF-token") {
                Then("returnerer 403") {
                    mockMvc.perform(get("${API_V1}/tema").header(AUTHORIZATION, ccJwt()))
                        .andExpect {
                            status().isForbidden
                        }
                }
            }
        }

        Given("beskyttet endepunkt ${API_V1}/tema/ansatt/{navIdent}") {
            When("request har OBO-token") {
                Then("returnerer 403") {
                    mockMvc.perform(get("${API_V1}/tema/ansatt/${TEST_ANSATT_ID}").header(AUTHORIZATION, oboJwt()))
                        .andExpect {
                            status().isForbidden
                        }
                }
            }

            When("request har gyldig CCF-token") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/tema/ansatt/${TEST_ANSATT_ID}").header(AUTHORIZATION, ccJwt()))
                        .andExpect {
                            status().isOk
                        }
                }
            }
        }

        Given("medlemsoppslag i utforskeren") {
            When("request mangler bearer-token") {
                Then("returnerer medlemmer som JSON") {
                    mockMvc.perform(get("$API_V1/gruppe/medlemmer").param("gruppeNavn", "test-gruppe"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].navIdent").value(TEST_ANSATT_ID.verdi))
                        .andExpect(jsonPath("$[0].visningNavn").value("Test Ansatt"))
                }
                Then("den statiske siden bruker gruppe-endepunktet") {
                    mockMvc.perform(get("/grupper.html"))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("/api/v1/gruppe/medlemmer?")))
                }
                Then("gruppe-endepunktet returnerer medlemmer uten token") {
                    mockMvc.perform(get("$API_V1/gruppe/medlemmer").param("gruppeNavn", "test-gruppe"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].navIdent").value(TEST_ANSATT_ID.verdi))
                        .andExpect(jsonPath("$[0].visningNavn").value("Test Ansatt"))
                }
            }
            When("gruppen ikke finnes") {
                Then("returnerer en tom liste") {
                    every { oidTjeneste.gruppeOid("ukjent-gruppe") } returns null
                    mockMvc.perform(get("$API_V1/gruppe/medlemmer").param("gruppeNavn", "ukjent-gruppe"))
                        .andExpect(status().isOk())
                        .andExpect(content().json("[]"))
                }
            }
        }

        Given("endepunkt ${API_V1}/enhet/{enhetsnummer}") {
            When("request har gyldig gruppe") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/enhet/${TEST_ENHET.enhetnummer.verdi}"))
                        .andExpect {
                            status().isOk
                        }
                }
            }
        }

        Given("endepunkt ${API_V1}/tema/{tema}") {
            When("request har gyldig gruppe") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/tema/AAP"))
                        .andExpect {
                            status().isOk
                        }
                }
            }
        }

        Given("endepunkt ${API_V1}/ansatt/{navIdent}") {
            When("request har gyldig navIdent") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/ansatt/${TEST_ANSATT_ID}"))
                        .andExpect {
                            status().isOk
                        }
                }
            }
        }

        Given("endepunkt ${API_V1}/ansatt/tident/{tIdent}") {
            When("request har gyldig tIdent") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/ansatt/tident/A123456"))
                        .andExpect {
                            status().isOk
                        }
                }
            }
        }

        Given("ubeskyttet endepunkt ${API_V1}/ansatt/tilganger/{navIdent}") {
            When("request mangler bearer-token") {
                Then("returnerer gruppemedlemskap") {
                    mockMvc.perform(get("${API_V1}/ansatt/tilganger/$TEST_ANSATT_ID"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].rolle").value("test-rolle"))
                }
                Then("ansattsiden henter og viser gruppemedlemskap") {
                    mockMvc.perform(get("/ansatt.html"))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("/api/v1/ansatt/tilganger/")))
                        .andExpect(content().string(containsString("Gruppemedlemskap")))
                }
            }
            When("request har OBO-token") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/ansatt/tilganger/${TEST_ANSATT_ID}").header(AUTHORIZATION, oboJwt()))
                        .andExpect {
                            status().isOk
                        }
                }
            }

            When("request har gyldig CCF-token") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/ansatt/tilganger/${TEST_ANSATT_ID}").header(AUTHORIZATION, ccJwt()))
                        .andExpect {
                            status().isOk
                        }
                }
            }
        }
    }


    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerMockOAuth2Server(registry: DynamicPropertyRegistry) {
            registry.setProperties(PROD_GCP)
        }
    }
}
