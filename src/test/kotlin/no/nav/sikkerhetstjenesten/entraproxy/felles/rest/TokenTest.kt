package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.CCF
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.Companion.from
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.OBO
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.UNAUTHENTICATED
import java.util.UUID

class TokenTest : BehaviorSpec({

    fun tokenMedClaims(oid: String? = null, navIdent: String? = null, idtyp: String? = null, azpName: String? = null): Token {
        val claims = mockk<JwtTokenClaims>(relaxed = true)
        every { claims.getStringClaim("oid") } returns oid
        every { claims.getStringClaim("NAVident") } returns navIdent
        every { claims.getStringClaim("idtyp") } returns idtyp
        every { claims.getStringClaim("azp_name") } returns azpName
        val ctx = mockk<TokenValidationContext>(relaxed = true)
        every { ctx.getClaims("azuread") } returns claims
        val holder = mockk<TokenValidationContextHolder>(relaxed = true)
        every { holder.getTokenValidationContext() } returns ctx
        return Token(holder)
    }

    Given("Token claim-utlesing") {
        When("alle claims er satt for CC-token") {
            val t = tokenMedClaims(azpName = "dev-fss:team:app", idtyp = "app")
            Then("system er azp_name") { t.system shouldBe "dev-fss:team:app" }
            Then("cluster er forste segment") { t.cluster shouldBe "dev-fss" }
            Then("systemNavn er siste segment") { t.systemNavn shouldBe "app" }
            Then("systemAndNs er alt etter cluster") { t.systemAndNs shouldBe "team:app" }
            Then("clusterAndSystem snur til app:cluster") { t.clusterAndSystem shouldBe "app:dev-fss" }
            Then("erCC er true") { t.erCC shouldBe true }
            Then("erObo er false") { t.erObo shouldBe false }
        }
        When("OBO-token med oid og navIdent") {
            val oid = UUID.randomUUID()
            val t = tokenMedClaims(oid = "$oid", navIdent = "A123456", azpName = "dev-fss:team:app")
            Then("oid er parset til UUID") { t.oid shouldBe oid }
            Then("ansattId er parset") { t.ansattId?.verdi shouldBe "A123456" }
            Then("erCC er false") { t.erCC shouldBe false }
            Then("erObo er true") { t.erObo shouldBe true }
        }
        When("token uten claims") {
            val t = tokenMedClaims()
            Then("system defaulter til N/A") { t.system shouldBe "N/A" }
            Then("oid er null") { t.oid shouldBe null }
            Then("ansattId er null") { t.ansattId shouldBe null }
            Then("erCC er false") { t.erCC shouldBe false }
            Then("erObo er false") { t.erObo shouldBe false }
        }
        When("system har faerre enn 3 segmenter") {
            val t = tokenMedClaims(azpName = "kun-en-del")
            Then("clusterAndSystem returnerer original system") {
                t.clusterAndSystem shouldBe "kun-en-del"
            }
            Then("cluster er forste segment") { t.cluster shouldBe "kun-en-del" }
            Then("systemNavn er siste segment") { t.systemNavn shouldBe "kun-en-del" }
        }
    }

    Given("Token.assert") {
        val t = tokenMedClaims(azpName = "x:y:z", idtyp = "app")
        When("predikat er sant") {
            Then("blokken kjores og resultat returneres") {
                t.assert({ erCC }, { setOf("ok") }) shouldBe setOf("ok")
            }
        }
        When("predikat er usant") {
            Then("kaster IllegalArgumentException") {
                io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                    t.assert({ erObo }, { setOf("ok") })
                }
            }
        }
    }

    Given("TokenType.from") {
        When("erObo er true") {
            Then("returnerer OBO") {
                val oid = UUID.randomUUID()
                val t = tokenMedClaims(oid = oid.toString(), navIdent = "A123456")
                from(t) shouldBe OBO
            }
        }
        When("erCC er true") {
            Then("returnerer CCF") {
                val t = tokenMedClaims(idtyp = "app")
                from(t) shouldBe CCF
            }
        }
        When("hverken erObo eller erCC") {
            Then("returnerer UNAUTHENTICATED") {
                val t = tokenMedClaims()
                from(t) shouldBe UNAUTHENTICATED
            }
        }
    }
})

