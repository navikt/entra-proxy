package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

class EnhetTest : BehaviorSpec({

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

    Given("Enhetnummer likhet og sortering") {
        val e1 = Enhetnummer("1234")
        val e1Lik = Enhetnummer("1234")
        val e2 = Enhetnummer("5678")
        val e1MedPrefix = Enhetnummer("${ENHET_PREFIX}1234")

        When("equals") {
            Then("samme nummer gir likhet") { e1 shouldBe e1Lik }
            Then("ulikt nummer gir ulikhet") { e1 shouldNotBe e2 }
            Then("konstruktor-input avgjor likhet (med vs uten prefix)") {
                e1.verdi shouldBe e1MedPrefix.verdi
                e1 shouldNotBe e1MedPrefix
            }
        }
        When("compareTo") {
            Then("sortering folger verdi") {
                listOf(e2, e1).sorted() shouldBe listOf(e1, e2)
            }
        }
    }

    Given("Enhet") {
        val n1 = Enhetnummer("1234")
        val n2 = Enhetnummer("5678")
        val a = Enhet(n1, "NAV Eksempel")
        val aLik = Enhet(n1, "NAV Eksempel")
        val annetNavn = Enhet(n1, "NAV Annet")
        val annetNummer = Enhet(n2, "NAV Eksempel")

        When("equals") {
            Then("samme verdier gir likhet") { a shouldBe aLik }
            Then("ulikt navn gir ulikhet") { a shouldNotBe annetNavn }
            Then("ulikt enhetnummer gir ulikhet") { a shouldNotBe annetNummer }
            Then("ulikhet mot null") { a.equals(null) shouldBe false }
        }
        When("hashCode") {
            Then("baserer seg kun pa enhetnummer (dokumenterer faktisk adferd)") {
                a.hashCode() shouldBe annetNavn.hashCode()
            }
        }
        When("compareTo") {
            Then("sortering folger enhetnummer") {
                listOf(annetNummer, a).sorted() shouldBe listOf(a, annetNummer)
            }
        }
    }
})

