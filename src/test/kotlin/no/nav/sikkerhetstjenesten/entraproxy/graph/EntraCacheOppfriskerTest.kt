package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel

class EntraCacheOppfriskerTest : BehaviorSpec({

    Given("en GRAPH cache-nøkkel for en metode som tar AnsattId") {
        When("nøkkelen peker på tema") {
            val entra = mockk<EntraTjeneste>(relaxed = true)
            EntraCacheOppfrisker(entra).oppfrisk(CacheNøkkel("graph::tema:A123456"))

            Then("oppfriskes tema med AnsattId") {
                verify(exactly = 1) { entra.tema(AnsattId("A123456")) }
            }
        }

        When("nøkkelen peker på enheter") {
            val entra = mockk<EntraTjeneste>(relaxed = true)
            EntraCacheOppfrisker(entra).oppfrisk(CacheNøkkel("graph::enheter:B123456"))

            Then("oppfriskes enheter med AnsattId") {
                verify(exactly = 1) { entra.enheter(AnsattId("B123456")) }
            }
        }

        When("nøkkelen peker på grupper") {
            val entra = mockk<EntraTjeneste>(relaxed = true)
            EntraCacheOppfrisker(entra).oppfrisk(CacheNøkkel("graph::grupper:C123456"))

            Then("oppfriskes grupper med AnsattId") {
                verify(exactly = 1) { entra.grupper(AnsattId("C123456")) }
            }
        }
    }

    Given("en GRAPH cache-nøkkel for utvidetAnsatt") {
        When("id-en har AnsattId-format") {
            val entra = mockk<EntraTjeneste>(relaxed = true)
            EntraCacheOppfrisker(entra).oppfrisk(CacheNøkkel("graph::utvidetAnsatt:A123456"))

            Then("velges AnsattId-overloaden") {
                verify(exactly = 1) { entra.utvidetAnsatt(AnsattId("A123456")) }
            }
        }

        When("id-en har TIdent-format") {
            val entra = mockk<EntraTjeneste>(relaxed = true)
            EntraCacheOppfrisker(entra).oppfrisk(CacheNøkkel("graph::utvidetAnsatt:ABC1234"))

            Then("velges TIdent-overloaden") {
                verify(exactly = 1) { entra.utvidetAnsatt(TIdent("ABC1234")) }
            }
        }
    }

    Given("en GRAPH cache-nøkkel med ukjent metode") {
        When("metoden ikke finnes på tjenesten") {
            val entra = mockk<EntraTjeneste>(relaxed = true)
            EntraCacheOppfrisker(entra).oppfrisk(CacheNøkkel("graph::ukjent:A123456"))

            Then("oppfriskes ingenting") {
                verify { entra wasNot Called }
            }
        }
    }

    Given("en GRAPH cache-nøkkel med ugyldig id-format") {
        When("id-en verken er AnsattId eller TIdent") {
            val entra = mockk<EntraTjeneste>(relaxed = true)
            EntraCacheOppfrisker(entra).oppfrisk(CacheNøkkel("graph::tema:1234567"))

            Then("oppfriskes ingenting") {
                verify { entra wasNot Called }
            }
        }
    }
})
