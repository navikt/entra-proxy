package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX

class TemaTest : BehaviorSpec({

    Given("Tema konstruksjon") {
        When("verdi har 3 bokstaver uten prefix") {
            val t = Tema("AAP")
            Then("verdi er uendret") { t.verdi shouldBe "AAP" }
            Then("gruppeNavn har prefix") { t.gruppeNavn shouldBe "${TEMA_PREFIX}AAP" }
        }
        When("verdi har prefix") {
            val t = Tema("${TEMA_PREFIX}AAP")
            Then("verdi har prefix fjernet") { t.verdi shouldBe "AAP" }
            Then("gruppeNavn har prefix") { t.gruppeNavn shouldBe "${TEMA_PREFIX}AAP" }
        }
        When("verdi etter prefix-fjerning ikke er 3 bokstaver") {
            Then("2 bokstaver kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { Tema("AA") }
            }
            Then("4 bokstaver kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { Tema("AAPA") }
            }
            Then("sifre kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { Tema("123") }
            }
            Then("blandet kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { Tema("A1P") }
            }
        }
    }

    Given("Tema sortering") {
        When("compareTo brukes") {
            Then("sortering folger verdi") {
                listOf(Tema("BBB"), Tema("AAA")).sorted().map { it.verdi } shouldBe listOf("AAA", "BBB")
            }
        }
    }
})

