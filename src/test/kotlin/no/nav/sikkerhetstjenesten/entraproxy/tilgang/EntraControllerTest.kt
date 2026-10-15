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
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomAnsatt
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomTjeneste
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
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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

    @MockkBean
    private lateinit var nomTjeneste: NomTjeneste

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

        Given("en side som ikke finnes") {
            Then("returnerer 404 uten å kreve autentisering") {
                mockMvc.perform(get("/does-not-exist"))
                    .andExpect(status().isNotFound())
            }
            Then("krever autentisering for et ukjent API-endepunkt") {
                mockMvc.perform(get("$API_V1/does-not-exist"))
                    .andExpect(status().isUnauthorized())
            }
            Then("viser en vennlig feilside i nettleseren") {
                mockMvc.perform(get("/grupper1.html").accept(TEXT_HTML))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(TEXT_HTML))
                    .andExpect(content().string(containsString("Siden finnes ikke")))
                    .andExpect(content().string(containsString("href=\"/\"")))
            }
            Then("beholder standard feilbehandling for en JSON-klient") {
                mockMvc.perform(get("/grupper1.html").accept(APPLICATION_JSON))
                    .andExpect(status().isNotFound())
            }
        }

        Given("NOM-stier uten eksplisitt offentlig tilgang") {
            Then("krever autentisering for andre GET-stier") {
                mockMvc.perform(get("$API_V1/nom/does-not-exist"))
                    .andExpect(status().isUnauthorized())
            }
            Then("krever autentisering for andre POST-stier") {
                mockMvc.perform(post("$API_V1/nom/does-not-exist"))
                    .andExpect(status().isUnauthorized())
            }
            Then("krever autentisering for POST mot et offentlig GET-endepunkt") {
                mockMvc.perform(post("$API_V1/nom/enhet/$TEST_ANSATT_ID"))
                    .andExpect(status().isUnauthorized())
            }
        }

        Given("forsiden (index.html på rot)") {
            When("request mangler innlogget sesjon") {
                Then("returnerer 401 slik at Wonderwall kan starte innlogging") {
                    mockMvc.perform(get("/"))
                        .andExpect(status().isUnauthorized())
                    mockMvc.perform(get("/index.html"))
                        .andExpect(status().isUnauthorized())
                }
            }
            When("request har gyldig token") {
                Then("serverer utforskersiden") {
                    mockMvc.perform(get("/").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                    mockMvc.perform(get("/index.html").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("Utforsker")))
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
            When("request har innlogget sesjon") {
                Then("returnerer medlemmer som JSON") {
                    mockMvc.perform(get("$API_V1/gruppe/medlemmer").param("gruppeNavn", "test-gruppe").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].navIdent").value(TEST_ANSATT_ID.verdi))
                        .andExpect(jsonPath("$[0].visningNavn").value("Test Ansatt"))
                }
                Then("den statiske siden bruker gruppe-endepunktet") {
                    mockMvc.perform(get("/grupper.html").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("/api/v1/gruppe/medlemmer?")))
                        .andExpect(content().string(containsString("Nom tilgangsenhet")))
                        .andExpect(content().string(containsString("/api/v1/nom/enhet/bulk")))
                        .andExpect(content().string(containsString("/api/v1/nom/ansatte/")))
                        .andExpect(content().string(containsString("/api/v1/nom/ansatte/enhet/")))
                        .andExpect(content().string(containsString("name=\"tilgangsenhetId\"")))
                        .andExpect(content().string(containsString("pattern=\"[0-9]{4}|[a-z]{2}[0-9]{3}[a-z]\"")))
                }
                Then("krever innlogget sesjon ved direkte tilgang") {
                    mockMvc.perform(get("/grupper.html"))
                        .andExpect(status().isUnauthorized())
                }
                Then("gruppe-endepunktet returnerer medlemmer med gyldig token") {
                    mockMvc.perform(get("$API_V1/gruppe/medlemmer").param("gruppeNavn", "test-gruppe").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].navIdent").value(TEST_ANSATT_ID.verdi))
                        .andExpect(jsonPath("$[0].visningNavn").value("Test Ansatt"))
                }
            }
            When("request mangler både sesjon og token") {
                Then("returnerer 401") {
                    mockMvc.perform(get("$API_V1/gruppe/medlemmer").param("gruppeNavn", "test-gruppe"))
                        .andExpect(status().isUnauthorized())
                }
            }
            When("gruppen ikke finnes") {
                Then("returnerer en tom liste") {
                    every { oidTjeneste.gruppeOid("ukjent-gruppe") } returns null
                    mockMvc.perform(get("$API_V1/gruppe/medlemmer").param("gruppeNavn", "ukjent-gruppe").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(content().json("[]"))
                }
            }
        }

        Given("antall medlemmer i gruppe") {
            When("request har innlogget sesjon") {
                Then("returnerer antall som tekst") {
                    every { entraTjeneste.antallMedlemmerIGruppe(any()) } returns "1535"
                    mockMvc.perform(get("$API_V1/gruppe/antall").param("gruppeNavn", "test-gruppe").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(content().string("1535"))
                }
            }
        }

        Given("antall medlemmer for flere grupper") {
            When("request har innlogget sesjon") {
                Then("returnerer antall per gruppe som JSON") {
                    val oid1 = UUID.randomUUID()
                    val oid2 = UUID.randomUUID()
                    every { oidTjeneste.gruppeOid("gruppe-1") } returns oid1
                    every { oidTjeneste.gruppeOid("gruppe-2") } returns oid2
                    every { entraTjeneste.antallMedlemmerIGrupper(*anyVararg()) } returns mapOf(oid1 to 10L, oid2 to 20L)
                    mockMvc.perform(
                        post("$API_V1/gruppe/antall/bulk")
                            .contentType(APPLICATION_JSON)
                            .content("[\"gruppe-1\",\"gruppe-2\"]")
                            .with(jwt())
                    )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$['gruppe-1']").value(10))
                        .andExpect(jsonPath("$['gruppe-2']").value(20))
                }
            }
        }

        Given("utforsker-endepunkt ${API_V1}/nom/enhet/{navIdent}") {
            When("request har innlogget sesjon") {
                Then("returnerer ansattdata og org-tilknytninger fra NOM") {
                    every { nomTjeneste.ansatt(TEST_ANSATT_ID) } returns NomAnsatt(
                        navident = TEST_ANSATT_ID,
                        visningsnavn = "Test Ansatt",
                        gjeldendeSektor = "STAT",
                        orgTilknytninger = setOf(
                            NomOrgTilknytning(
                                NomEnhet(
                                    NomIdent("ra656d"),
                                    "Testenhet",
                                    TEST_ENHET.enhetnummer
                                )
                            )
                        )
                    )
                    mockMvc.perform(get("${API_V1}/nom/enhet/${TEST_ANSATT_ID}").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.navident").value(TEST_ANSATT_ID.verdi))
                        .andExpect(jsonPath("$.visningsnavn").value("Test Ansatt"))
                        .andExpect(jsonPath("$.gjeldendeSektor").value("STAT"))
                        .andExpect(jsonPath("$.orgTilknytninger[0].orgEnhet.navn").value("Testenhet"))
                }
            }
        }

        Given("utforsker-endepunkt ${API_V1}/nom/enhet/bulk") {
            When("request mangler både sesjon og token") {
                Then("returnerer 401") {
                    mockMvc.perform(
                        post("${API_V1}/nom/enhet/bulk")
                            .contentType(APPLICATION_JSON)
                            .content("[\"${TEST_ANSATT_ID.verdi}\"]")
                    )
                        .andExpect(status().isUnauthorized())
                }
            }
            When("request har innlogget sesjon") {
                Then("returnerer org-tilknytninger for flere ansatte fra NOM") {
                    every { nomTjeneste.ansatte(any()) } returns setOf(
                        NomAnsatt(
                            navident = TEST_ANSATT_ID,
                            visningsnavn = "Test Ansatt",
                            gjeldendeSektor = "STAT",
                            orgTilknytninger = setOf(
                                NomOrgTilknytning(
                                    NomEnhet(
                                        NomIdent("ra656d"),
                                        "Testenhet",
                                        TEST_ENHET.enhetnummer
                                    )
                                )
                            )
                        )
                    )
                    mockMvc.perform(
                        post("${API_V1}/nom/enhet/bulk")
                            .contentType(APPLICATION_JSON)
                            .content("[\"${TEST_ANSATT_ID.verdi}\"]")
                            .header(AUTHORIZATION, "Bearer ${oboJwt()}")
                    )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].navident").value(TEST_ANSATT_ID.verdi))
                        .andExpect(jsonPath("$[0].visningsnavn").value("Test Ansatt"))
                        .andExpect(jsonPath("$[0].orgTilknytninger[0].orgEnhet.tilgangsenhetId").value(TEST_ENHET.enhetnummer.verdi))
                }
            }
        }

        Given("utforsker-endepunkt ${API_V1}/nom/ansatte/{tilgangsenhetId}") {
            When("request har innlogget sesjon") {
                Then("returnerer ansatte fra NOM") {
                    every { nomTjeneste.ansatteForEnhet(TEST_ENHET.enhetnummer) } returns setOf(
                        NomAnsatt(
                            navident = TEST_ANSATT_ID,
                            visningsnavn = "Test Ansatt",
                            gjeldendeSektor = "STAT"
                        )
                    )
                    mockMvc.perform(get("${API_V1}/nom/ansatte/${TEST_ENHET.enhetnummer.verdi}").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].navident").value(TEST_ANSATT_ID.verdi))
                        .andExpect(jsonPath("$[0].visningsnavn").value("Test Ansatt"))
                }
            }
        }

        Given("endepunkt ${API_V1}/enhet/{enhetsnummer}") {
            When("request har gyldig gruppe") {
                Then("ansatt.html krever innlogging") {
                    mockMvc.perform(get("/ansatt.html"))
                        .andExpect(status().isUnauthorized())
                }
                Then("ansatt.html er tilgjengelig i prod") {
                    mockMvc.perform(get("/ansatt.html").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("/api/v1/gruppe/antall?")))
                        .andExpect(content().string(containsString("Antall medlemmer")))
                        .andExpect(content().string(containsString("/api/v1/ansatt/tilganger/")))
                        .andExpect(content().string(containsString("Gruppemedlemskap")))
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
            When("request har innlogget sesjon") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/ansatt/${TEST_ANSATT_ID}").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                }
            }
            When("request mangler både sesjon og token") {
                Then("returnerer 401") {
                    mockMvc.perform(get("${API_V1}/ansatt/${TEST_ANSATT_ID}"))
                        .andExpect(status().isUnauthorized())
                }
            }
        }

        Given("endepunkt ${API_V1}/ansatt/tident/{tIdent}") {
            When("request har gyldig token") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/ansatt/tident/A123456").header(AUTHORIZATION, "Bearer ${ccJwt()}"))
                        .andExpect(status().isOk())
                }
            }
        }

        Given("utforsker-endepunkt ${API_V1}/ansatt/tilganger/{navIdent}") {
            When("request har innlogget sesjon") {
                Then("returnerer gruppemedlemskap") {
                    mockMvc.perform(get("${API_V1}/ansatt/tilganger/$TEST_ANSATT_ID").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$[0].rolle").value("test-rolle"))
                }
                Then("ansatt.html er tilgjengelig i prod") {
                    mockMvc.perform(get("/ansatt.html").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                }
            }
            When("request mangler både sesjon og token") {
                Then("returnerer 401") {
                    mockMvc.perform(get("${API_V1}/ansatt/tilganger/$TEST_ANSATT_ID"))
                        .andExpect(status().isUnauthorized())
                }
            }
            When("request har OBO-token") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/ansatt/tilganger/${TEST_ANSATT_ID}").header(AUTHORIZATION, "Bearer ${oboJwt()}"))
                        .andExpect(status().isOk())
                }
            }

            When("request har gyldig CCF-token") {
                Then("returnerer 200") {
                    mockMvc.perform(get("${API_V1}/ansatt/tilganger/${TEST_ANSATT_ID}").header(AUTHORIZATION, "Bearer ${ccJwt()}"))
                        .andExpect(status().isOk())
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
