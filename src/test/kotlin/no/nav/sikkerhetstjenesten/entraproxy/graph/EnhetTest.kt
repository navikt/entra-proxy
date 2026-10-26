package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

class EnhetTest : BehaviorSpec({

    Given("Enhetnummer.parse") {
        When("verdien er gyldig med eller uten prefiks") {
            mapOf(
                "1234" to "1234",
                "0000" to "0000",
                "${ENHET_PREFIX}1234" to "1234",
                "${ENHET_PREFIX}0000" to "0000",
            ).forEach { (verdi, forventetNummer) ->
                Then("returneres Enhetnummer for '$verdi'") {
                    val resultat = Enhetnummer.parse(verdi)
                    resultat shouldBe Enhetnummer(verdi)
                    resultat?.verdi shouldBe forventetNummer
                    resultat?.gruppeNavn shouldBe "$ENHET_PREFIX$forventetNummer"
                }
            }
        }
        When("verdien er ugyldig") {
            listOf(
                "", "123", "12345", "12AB", "123 ", " 1234",
                ENHET_PREFIX, "${ENHET_PREFIX}123", "${ENHET_PREFIX}12345", "${ENHET_PREFIX}12AB",
            ).forEach { verdi ->
                Then("returneres null for '$verdi'") {
                    Enhetnummer.parse(verdi).shouldBeNull()
                }
            }
        }
    }

    Given("Enhetnummer konstruksjon") {
        When("verdi har 4 sifre uten prefix") {
            val e = Enhetnummer("1234")
            Then("verdi er nummeret") { e.verdi shouldBe "1234" }
            Then("gruppeNavn har prefix") { e.gruppeNavn shouldBe "${ENHET_PREFIX}1234" }
        }
        When("verdi har prefix") {
            val e = Enhetnummer("${ENHET_PREFIX}1234")
            Then("verdi har prefix fjernet") { e.verdi shouldBe "1234" }
            Then("gruppeNavn har prefix") { e.gruppeNavn shouldBe "${ENHET_PREFIX}1234" }
        }
        When("verdi etter prefix-fjerning ikke er 4 sifre") {
            Then("3 sifre kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { Enhetnummer("123") }
            }
            Then("5 sifre kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { Enhetnummer("12345") }
            }
            Then("bokstaver kaster IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { Enhetnummer("12AB") }
            }
        }
    }
})
