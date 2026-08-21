package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.APP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.AZP_NAME
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.IDTYP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.OID
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.CCF
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.OBO
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.UNAUTHENTICATED
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.UTILGJENGELIG
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID

class TokenTest : BehaviorSpec({

    val token = Token()
    val oid = UUID.randomUUID()

    beforeEach {
        SecurityContextHolder.clearContext()
    }

    afterEach {
        SecurityContextHolder.clearContext()
    }

    fun setJwtClaims(claims: Map<String, Any>) {
        val jwtClaims = mutableMapOf<String, Any>()
        jwtClaims.putAll(claims)

        val jwt = Jwt(
            "token",
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(60),
            mapOf("alg" to "none"),
            jwtClaims
        )

        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(jwt, "credentials")
    }

    Given("type") {
        When("idtyp er 'app'") {
            Then("token er CCF") {
                setJwtClaims(mapOf(IDTYP to APP))
                token.type shouldBe CCF
            }
        }
        When("oid finnes og idtyp ikke er 'app'") {
            Then("token er OBO") {
                setJwtClaims(mapOf(OID to oid.toString(), IDTYP to "user"))
                token.type shouldBe OBO
            }
        }
        When("ingen claims finnes") {
            Then("token er UNAUTHENTICATED") {
                token.type shouldBe UNAUTHENTICATED
            }
        }
    }

    Given("ansattId") {
        When("NAVident finnes") {
            Then("returnerer AnsattId") {
                setJwtClaims(mapOf(NAVIDENT to "Z999999"))
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
                setJwtClaims(mapOf(OID to oid.toString()))
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
                setJwtClaims(mapOf(AZP_NAME to "dev-gcp:team:app"))
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
                setJwtClaims(mapOf(AZP_NAME to "dev-gcp:team:app"))
                token.systemNavn shouldBe "app"
            }
        }
        When("azp_name er ett ord uten kolon") {
            Then("returnerer azp_name uendret") {
                setJwtClaims(mapOf(AZP_NAME to "app"))
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
                setJwtClaims(mapOf(AZP_NAME to "dev-gcp:team:app"))
                token.cluster shouldBe "dev-gcp"
            }
        }
        When("azp_name er ett ord uten kolon") {
            Then("returnerer azp_name uendret") {
                setJwtClaims(mapOf(AZP_NAME to "app"))
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
                setJwtClaims(mapOf(AZP_NAME to "dev-gcp:team:app"))
                token.systemAndNs shouldBe "team:app"
            }
        }
        When("azp_name har to deler") {
            Then("returnerer siste del") {
                setJwtClaims(mapOf(AZP_NAME to "dev-gcp:app"))
                token.systemAndNs shouldBe "app"
            }
        }
        When("azp_name er ett ord uten kolon") {
            Then("returnerer tom streng") {
                setJwtClaims(mapOf(AZP_NAME to "app"))
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
                setJwtClaims(mapOf(AZP_NAME to "dev-gcp:team:app"))
                token.clusterAndSystem shouldBe "app:dev-gcp"
            }
        }
        When("azp_name ikke har tre deler") {
            Then("returnerer system uendret") {
                setJwtClaims(mapOf(AZP_NAME to "app"))
                token.clusterAndSystem shouldBe "app"
            }
        }
    }

    Given("ingen gyldig token-kontekst") {
        When("ingen authentication finnes") {
            Then("er tom/ikke autentisert") {
                token.type shouldBe UNAUTHENTICATED
                token.ansattId shouldBe null
                token.system shouldBe UTILGJENGELIG
            }
        }
    }
})
