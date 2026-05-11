package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

class AnsattTest : BehaviorSpec({

    Given("Ansatt") {
        val a1 = Ansatt(NAVIDENT_A, "Ola Nordmann", "Ola", "Nordmann")
        val a1Lik = Ansatt(NAVIDENT_A, "Ola Nordmann", "Ola", "Nordmann")
        val annenNavIdent = Ansatt(NAVIDENT_B, "Ola Nordmann", "Ola", "Nordmann")
        val annetVisningNavn = Ansatt(NAVIDENT_A, "Annet Navn", "Ola", "Nordmann")
        val annetFornavn = Ansatt(NAVIDENT_A, "Ola Nordmann", "Per", "Nordmann")
        val annetEtternavn = Ansatt(NAVIDENT_A, "Ola Nordmann", "Ola", "Hansen")

        When("equals") {
            Then("samme instans er lik seg selv") { a1 shouldBe a1 }
            Then("samme feltverdier gir likhet") { a1 shouldBe a1Lik }
            Then("ulik navIdent gir ulikhet") { a1 shouldNotBe annenNavIdent }
            Then("ulikt visningNavn gir ulikhet") { a1 shouldNotBe annetVisningNavn }
            Then("ulikt fornavn gir ulikhet") { a1 shouldNotBe annetFornavn }
            Then("ulikt etternavn gir ulikhet") { a1 shouldNotBe annetEtternavn }
            Then("ulikhet mot null") { (a1.equals(null)) shouldBe false }
            Then("ulikhet mot fremmed type") { (a1.equals("noe")) shouldBe false }
        }

        When("hashCode") {
            Then("like objekter har samme hashCode") {
                a1.hashCode() shouldBe a1Lik.hashCode()
            }
            Then("hashCode er stabil over flere kall") {
                a1.hashCode() shouldBe a1.hashCode()
            }
        }

        When("compareTo") {
            Then("sortering følger navIdent") {
                listOf(annenNavIdent, a1).sorted() shouldContainExactly listOf(a1, annenNavIdent)
            }
            Then("returnerer 0 ved samme navIdent selv om andre felt er ulike") {
                a1.compareTo(annetVisningNavn) shouldBe 0
            }
        }

        When("default-verdier brukes") {
            val medDefaults = Ansatt(NAVIDENT_A)
            Then("visningNavn, fornavn, etternavn settes til UKJENT") {
                medDefaults.visningNavn shouldBe UKJENT
                medDefaults.fornavn shouldBe UKJENT
                medDefaults.etternavn shouldBe UKJENT
            }
        }
    }

    Given("UtvidetAnsatt") {
        val u1 = UtvidetAnsatt(NAVIDENT_A, "Ola Nordmann", "Ola", "Nordmann", TIDENT_A, "ola@nav.no", ENHET)
        val u1Lik = UtvidetAnsatt(NAVIDENT_A, "Ola Nordmann", "Ola", "Nordmann", TIDENT_A, "ola@nav.no", ENHET)
        val ansattMedSammeBaseFelt = Ansatt(NAVIDENT_A, "Ola Nordmann", "Ola", "Nordmann")

        When("equals") {
            Then("samme verdier gir likhet") { u1 shouldBe u1Lik }
            Then("symmetri mot Ansatt med samme basisfelt") {
                // Avdekker mulig bug: UtvidetAnsatt overrider ikke equals,
                // så likhet mot Ansatt er kun basert på basisfeltene.
                (u1 == ansattMedSammeBaseFelt) shouldBe (ansattMedSammeBaseFelt == u1)
            }
        }

        When("compareTo") {
            Then("sammenligning på navIdent fungerer på tvers av Ansatt og UtvidetAnsatt") {
                val annen = UtvidetAnsatt(NAVIDENT_B, null, null, null, TIDENT_A, null, ENHET)
                listOf(annen, u1).sorted() shouldContainExactly listOf(u1, annen)
            }
        }
    }
}) {
    companion object {
        private val NAVIDENT_A = AnsattId("A123456")
        private val NAVIDENT_B = AnsattId("B123456")
        private val TIDENT_A = TIdent("T111111")
        private val ENHET = Enhet(Enhetnummer("1234"), "NAV Enhet")
    }
}

