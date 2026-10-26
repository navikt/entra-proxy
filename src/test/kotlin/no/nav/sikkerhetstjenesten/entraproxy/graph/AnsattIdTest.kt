package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AnsattIdTest : BehaviorSpec({

    Given("AnsattId.parse") {
        When("verdien er gyldig") {
            listOf("A123456", "Z000000", "B999999").forEach { verdi ->
                Then("returneres AnsattId for '$verdi'") {
                    AnsattId.parse(verdi) shouldBe AnsattId(verdi)
                }
            }
        }
        When("verdien er ugyldig") {
            listOf("", "A12345", "A1234567", "1234567", "#123456", "A12B456", "A12345 ", " A123456").forEach { verdi ->
                Then("returneres null for '$verdi'") {
                    AnsattId.parse(verdi).shouldBeNull()
                }
            }
        }
    }

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
})
