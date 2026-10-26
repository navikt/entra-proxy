package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TIdentTest : BehaviorSpec({

    Given("TIdent.parse") {
        When("verdien er gyldig") {
            listOf("ABC1234", "XYZ0000", "DEF9999").forEach { verdi ->
                Then("returneres TIdent for '$verdi'") {
                    TIdent.parse(verdi) shouldBe TIdent(verdi)
                }
            }
        }
        When("verdien er ugyldig") {
            listOf("", "ABC123", "ABC12345", "1BC1234", "#BC1234", "ABC12A4", "ABC123 ", " ABC1234").forEach { verdi ->
                Then("returneres null for '$verdi'") {
                    TIdent.parse(verdi).shouldBeNull()
                }
            }
        }
    }

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
                shouldThrow<IllegalArgumentException> {
                    TIdent("ABC12345")
                }
            }
            Then("tom streng kaster IllegalArgumentException") {
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
})
