package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.AAD_ISSUER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.APP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.AZP_NAME
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.IDTYP
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.OID
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.UTILGJENGELIG
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import java.util.UUID


class TokenTest : BehaviorSpec({

    val contextHolder = mockk<TokenValidationContextHolder>()
    val validationContext = mockk<TokenValidationContext>()
    val claims = mockk<JwtTokenClaims>()
    val token = Token(contextHolder)

    val oid = UUID.randomUUID()

    beforeEach {
        every { contextHolder.getTokenValidationContext() } returns validationContext
        every { validationContext.getClaims(AAD_ISSUER) } returns claims
        every { claims.getStringClaim(any()) } returns null
        every { claims.getAsList(any()) } returns emptyList()
    }

    Given("erCC") {
        When("idtyp er 'app'") {
            Then("CC er true") {
                every { claims.getStringClaim(IDTYP) } returns APP
                token.erCC.shouldBeTrue()
            }
        }
        When("idtyp ikke er 'app'") {
            Then("CC er false") {
                every { claims.getStringClaim(IDTYP) } returns "user"
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
                every { claims.getStringClaim(OID) } returns oid.toString()
                token.erObo.shouldBeTrue()
            }
        }
        When("token er CC (idtyp=app)") {
            Then("OBO er false") {
                every { claims.getStringClaim(IDTYP) } returns APP
                every { claims.getStringClaim(OID) } returns oid.toString()
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
                every { claims.getStringClaim(NAVIDENT) } returns "Z999999"
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
                every { claims.getStringClaim(OID) } returns oid.toString()
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
                every { claims.getStringClaim(AZP_NAME) } returns "dev-gcp:team:app"
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
                every { claims.getStringClaim(AZP_NAME) } returns "dev-gcp:team:app"
                token.systemNavn shouldBe "app"
            }
        }
        When("azp_name er ett ord uten kolon") {
            Then("returnerer azp_name uendret") {
                every { claims.getStringClaim(AZP_NAME) } returns "app"
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
                every { claims.getStringClaim(AZP_NAME) } returns "dev-gcp:team:app"
                token.cluster shouldBe "dev-gcp"
            }
        }
        When("azp_name er ett ord uten kolon") {
            Then("returnerer azp_name uendret") {
                every { claims.getStringClaim(AZP_NAME) } returns "app"
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
                every { claims.getStringClaim(AZP_NAME) } returns "dev-gcp:team:app"
                token.systemAndNs shouldBe "team:app"
            }
        }
        When("azp_name har to deler") {
            Then("returnerer siste del") {
                every { claims.getStringClaim(AZP_NAME) } returns "dev-gcp:app"
                token.systemAndNs shouldBe "app"
            }
        }
        When("azp_name er ett ord uten kolon") {
            Then("returnerer tom streng") {
                every { claims.getStringClaim(AZP_NAME) } returns "app"
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
                every { claims.getStringClaim(AZP_NAME) } returns "dev-gcp:team:app"
                token.clusterAndSystem shouldBe "app:dev-gcp"
            }
        }
        When("azp_name ikke har tre deler") {
            Then("returnerer system uendret") {
                every { claims.getStringClaim(AZP_NAME) } returns "app"
                token.clusterAndSystem shouldBe "app"
            }
        }
    }


    Given("ingen gyldig token-kontekst") {
        beforeEach {
            every { validationContext.getClaims(AAD_ISSUER) } throws RuntimeException("ingen token")
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
                every { claims.getStringClaim(OID) } returns oid.toString()
                TokenType.from(token) shouldBe TokenType.OBO
            }
        }
        When("token er CC") {
            Then("returnerer CCF") {
                every { claims.getStringClaim(IDTYP) } returns APP
                TokenType.from(token) shouldBe TokenType.CCF
            }
        }
        When("ingen claims finnes") {
            Then("returnerer UNAUTHENTICATED") {
                every { validationContext.getClaims(AAD_ISSUER) } throws RuntimeException("ingen token")
                TokenType.from(token) shouldBe TokenType.UNAUTHENTICATED
            }
        }
    }
})
