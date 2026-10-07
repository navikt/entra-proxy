package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NomIdentTest : BehaviorSpec({

    Given("NomIdent konstruksjon") {
        When("verdi har gyldig format (2 små bokstaver + 3 sifre + 1 liten bokstav)") {
            Then("opprettes uten feil") {
                NomIdent("ra656d").verdi shouldBe "ra656d"
            }
        }
        When("verdi har feil lengde") {
            Then("for kort kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { NomIdent("ra656") }
            }
            Then("for lang kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { NomIdent("ra656dd") }
            }
            Then("tom string kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { NomIdent("") }
            }
        }
        When("de to første tegnene ikke er små bokstaver") {
            Then("store bokstaver kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { NomIdent("RA656d") }
            }
            Then("siffer kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { NomIdent("12656d") }
            }
        }
        When("de tre midterste tegnene ikke er sifre") {
            Then("bokstav blant sifrene kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { NomIdent("ra6a6d") }
            }
        }
        When("siste tegn ikke er en liten bokstav") {
            Then("stor bokstav kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { NomIdent("ra656D") }
            }
            Then("siffer kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { NomIdent("ra6560") }
            }
        }
    }

    Given("NomIdent likhet og sortering") {
        val a = NomIdent("ra656d")
        val aLik = NomIdent("ra656d")
        val b = NomIdent("rb656d")

        When("equals") {
            Then("samme verdi gir likhet") { a shouldBe aLik }
            Then("ulik verdi gir ulikhet") { a shouldNotBe b }
        }
        When("compareTo") {
            Then("sortering følger verdi") {
                listOf(b, a).sorted() shouldBe listOf(a, b)
            }
            Then("samme verdi gir 0") {
                a.compareTo(aLik) shouldBe 0
            }
        }
        When("toString") {
            Then("returnerer verdi") { a.toString() shouldBe "ra656d" }
        }
    }
})
