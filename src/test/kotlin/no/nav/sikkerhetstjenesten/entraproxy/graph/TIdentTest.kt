package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TIdentTest : BehaviorSpec({

    Given("TIdent konstruksjon") {
        When("verdi har gyldig format (3 bokstaver + 4 sifre)") {
            Then("opprettes uten feil") {
                TIdent("ABC1234").verdi shouldBe "ABC1234"
            }
        }
        When("verdi har feil lengde") {
            Then("for kort kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { TIdent("ABC123") }
            }
            Then("for lang kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { TIdent("ABC12345") }
            }
            Then("tom string kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { TIdent("") }
            }
        }
        When("forste tegn ikke er bokstav") {
            Then("siffer som forste tegn kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { TIdent("1BC1234") }
            }
        }
        When("siste fire tegn ikke er sifre") {
            Then("bokstav blant sifrene kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { TIdent("ABC12A4") }
            }
        }
    }

    Given("TIdent likhet") {
        val t = TIdent("ABC1234")
        val tLik = TIdent("ABC1234")
        val annen = TIdent("DEF1234")

        When("equals") {
            Then("samme verdi gir likhet") { t shouldBe tLik }
            Then("ulik verdi gir ulikhet") { t shouldNotBe annen }
        }
        When("toString") {
            Then("returnerer verdi") { t.toString() shouldBe "ABC1234" }
        }
    }
})

