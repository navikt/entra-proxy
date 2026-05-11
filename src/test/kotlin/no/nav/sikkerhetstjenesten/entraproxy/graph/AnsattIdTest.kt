package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AnsattIdTest : BehaviorSpec({

    Given("AnsattId konstruksjon") {
        When("verdi har gyldig format (1 bokstav + 6 sifre)") {
            Then("opprettes uten feil") {
                AnsattId("A123456").verdi shouldBe "A123456"
            }
        }
        When("verdi har feil lengde") {
            Then("for kort kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { AnsattId("A12345") }
            }
            Then("for lang kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { AnsattId("A1234567") }
            }
            Then("tom string kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { AnsattId("") }
            }
        }
        When("første tegn ikke er bokstav") {
            Then("siffer som første tegn kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { AnsattId("1234567") }
            }
            Then("spesialtegn som første tegn kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { AnsattId("#123456") }
            }
        }
        When("siste seks tegn ikke er sifre") {
            Then("bokstav blant sifrene kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { AnsattId("A12B456") }
            }
        }
    }

    Given("AnsattId likhet og sortering") {
        val a = AnsattId("A123456")
        val aLik = AnsattId("A123456")
        val b = AnsattId("B123456")

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
            Then("returnerer verdi") { a.toString() shouldBe "A123456" }
        }
    }
})

