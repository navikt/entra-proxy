package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.APP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.AZP_NAME
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.IDTYP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.OID
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.UTILGJENGELIG
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

private fun setClaims(vararg claims: Pair<String, String>) {
    val jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claims { map -> claims.forEach { map[it.first] = it.second } }
        .build()
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(jwt, null)
}


class TokenTest : BehaviorSpec({

    val token = Token()

    val oid = UUID.randomUUID()

    beforeEach {
        SecurityContextHolder.clearContext()
    }

    Given("erCC") {
        When("idtyp er 'app'") {
            Then("CC er true") {
                setClaims(IDTYP to APP)
                token.erCC.shouldBeTrue()
            }
        }
        When("idtyp ikke er 'app'") {
            Then("CC er false") {
                setClaims(IDTYP to "user")
                token.erCC shouldBe false
            }
        }
        When("idtyp mangler") {
            Then("CC er false") {
                token.erCC shouldBe false
            }
        }
    }

    Given("erObo") {
        When("oid finnes og idtyp ikke er 'app'") {
            Then("OBO er true") {
                setClaims(OID to oid.toString())
                token.erObo.shouldBeTrue()
            }
        }
        When("token er CC (idtyp=app)") {
            Then("OBO er false") {
                setClaims(IDTYP to APP, OID to oid.toString())
                token.erObo shouldBe false
            }
        }
        When("oid mangler") {
            Then("OBO er false") {
                token.erObo shouldBe false
            }
        }
    }

    Given("ansattId") {
        When("NAVident finnes") {
            Then("returnerer AnsattId") {
                setClaims(NAVIDENT to "Z999999")
                token.ansattId shouldBe AnsattId("Z999999")
            }
        }
        When("NAVident mangler") {
            Then("AnsattId er null") {
                token.ansattId shouldBe null
            }
        }
    }

    Given("oid-oppslag fra token") {
        When("oid finnes") {
            Then("returnerer oid") {
                setClaims(OID to oid.toString())
                token.oid shouldBe oid
            }
        }
        When("oid mangler") {
            Then("oid er null") {
                token.oid shouldBe null
            }
        }
    }

    Given("system") {
        When("azp_name finnes") {
            Then("returnerer azp_name") {
                setClaims(AZP_NAME to "dev-gcp:team:app")
                token.system shouldBe "dev-gcp:team:app"
            }
        }
        When("azp_name mangler") {
            Then("returnerer UTILGJENGELIG") {
                token.system shouldBe UTILGJENGELIG
            }
        }
    }

    Given("systemNavn") {
        When("azp_name har tre deler") {
            Then("returnerer siste del") {
                setClaims(AZP_NAME to "dev-gcp:team:app")
                token.systemNavn shouldBe "app"
            }
        }
        When("azp_name er ett ord uten kolon") {
            Then("returnerer azp_name uendret") {
                setClaims(AZP_NAME to "app")
                token.systemNavn shouldBe "app"
            }
        }
        When("azp_name mangler") {
            Then("returnerer UTILGJENGELIG") {
                token.systemNavn shouldBe UTILGJENGELIG
            }
        }
    }

    Given("cluster-informasjon fra token") {
        When("azp_name har tre deler") {
            Then("returnerer første del") {
                setClaims(AZP_NAME to "dev-gcp:team:app")
                token.cluster shouldBe "dev-gcp"
            }
        }
        When("azp_name er ett ord uten kolon") {
            Then("returnerer azp_name uendret") {
                setClaims(AZP_NAME to "app")
                token.cluster shouldBe "app"
            }
        }
        When("azp_name mangler") {
            Then("returnerer UTILGJENGELIG") {
                token.cluster shouldBe UTILGJENGELIG
            }
        }
    }

    Given("systemAndNs") {
        When("azp_name er cluster:namespace:app") {
            Then("returnerer namespace:app") {
                setClaims(AZP_NAME to "dev-gcp:team:app")
                token.systemAndNs shouldBe "team:app"
            }
        }
        When("azp_name har to deler") {
            Then("returnerer siste del") {
                setClaims(AZP_NAME to "dev-gcp:app")
                token.systemAndNs shouldBe "app"
            }
        }
        When("azp_name er ett ord uten kolon") {
            Then("returnerer tom streng") {
                setClaims(AZP_NAME to "app")
                token.systemAndNs shouldBe ""
            }
        }
        When("azp_name mangler") {
            Then("returnerer tom streng") {
                token.systemAndNs shouldBe ""
            }
        }
    }

    Given("clusterAndSystem") {
        When("azp_name har tre deler") {
            Then("returnerer 'app:cluster'") {
                setClaims(AZP_NAME to "dev-gcp:team:app")
                token.clusterAndSystem shouldBe "app:dev-gcp"
            }
        }
        When("azp_name ikke har tre deler") {
            Then("returnerer system uendret") {
                setClaims(AZP_NAME to "app")
                token.clusterAndSystem shouldBe "app"
            }
        }
    }


    Given("ingen gyldig token-kontekst") {
        beforeEach {
            SecurityContextHolder.clearContext()
        }
        When("getClaims kaster exception") {
            Then("erCC er false") {
                token.erCC shouldBe false
            }
            Then("erObo er false") {
                token.erObo shouldBe false
            }
            Then("ansattId er null") {
                token.ansattId shouldBe null
            }
        }
    }

    Given("TokenType.from") {
        When("token er OBO") {
            Then("returnerer OBO") {
                setClaims(OID to oid.toString())
                TokenType.from(token) shouldBe TokenType.OBO
            }
        }
        When("token er CC") {
            Then("returnerer CCF") {
                setClaims(IDTYP to APP)
                TokenType.from(token) shouldBe TokenType.CCF
            }
        }
        When("ingen claims finnes") {
            Then("returnerer UNAUTHENTICATED") {
                SecurityContextHolder.clearContext()
                TokenType.from(token) shouldBe TokenType.UNAUTHENTICATED
            }
        }
    }

})
