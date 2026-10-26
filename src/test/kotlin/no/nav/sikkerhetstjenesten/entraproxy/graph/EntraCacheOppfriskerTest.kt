package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere.EntraCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import java.net.URI
import java.time.Duration.ofHours

class EntraCacheOppfriskerTest : BehaviorSpec({

    val cfg = EntraConfig(URI("http://localhost"), ofHours(3))
    val entra = mockk<EntraTjeneste>(relaxed = true)
    val oppfrisker = EntraCacheOppfrisker(entra, cfg)

    beforeEach {
        clearMocks(entra)
    }

    Given("en GRAPH cache-nøkkel for en metode som tar AnsattId") {
        When("nøkkelen peker på tema") {
            Then("oppfriskes tema med AnsattId") {
                oppfrisker.oppfrisk(CacheNøkkel("graph::tema:A123456"))
                verify(exactly = 1) {
                    entra.tema(AnsattId("A123456"))
                }
            }
        }

        When("nøkkelen peker på enheter") {
            Then("oppfriskes enheter med AnsattId") {
                oppfrisker.oppfrisk(CacheNøkkel("graph::enheter:B123456"))
                verify(exactly = 1) {
                    entra.enheter(AnsattId("B123456"))
                }
            }
        }

        When("nøkkelen peker på grupper") {
            Then("oppfriskes grupper med AnsattId") {
                oppfrisker.oppfrisk(CacheNøkkel("graph::grupper:C123456"))
                verify(exactly = 1) {
                    entra.grupper(AnsattId("C123456"))
                }
            }
        }
    }

    Given("en GRAPH cache-nøkkel for utvidetAnsatt") {
        When("id-en har AnsattId-format") {
            Then("velges AnsattId-overloaden") {
                oppfrisker.oppfrisk(CacheNøkkel("graph::utvidetAnsatt:A123456"))
                verify(exactly = 1) {
                    entra.utvidetAnsatt(AnsattId("A123456"))
                }
            }
        }

        When("id-en har TIdent-format") {
            Then("velges TIdent-overloaden") {
                oppfrisker.oppfrisk(CacheNøkkel("graph::utvidetAnsatt:ABC1234"))
                verify(exactly = 1) {
                    entra.utvidetAnsatt(TIdent("ABC1234"))
                }
            }
        }
    }

    Given("en GRAPH cache-nøkkel med ukjent metode") {
        When("metoden ikke finnes på tjenesten") {
            Then("oppfriskes ingenting") {
                oppfrisker.oppfrisk(CacheNøkkel("graph::ukjent:A123456"))
                verify {
                    entra wasNot Called
                }
            }
        }
    }

    Given("en GRAPH cache-nøkkel med ugyldig id-format") {
        When("id-en verken er AnsattId eller TIdent") {
            Then("oppfriskes ingenting") {
                oppfrisker.oppfrisk(CacheNøkkel("graph::tema:1234567"))
                verify {
                    entra wasNot Called
                }
            }
        }
    }
})
