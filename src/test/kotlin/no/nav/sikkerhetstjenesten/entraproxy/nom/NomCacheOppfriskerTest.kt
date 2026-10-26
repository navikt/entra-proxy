package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel

class NomCacheOppfriskerTest : BehaviorSpec({

    val cfg = NomGraphQLConfig("localhost")
    val nom = mockk<NomTjeneste>(relaxed = true)
    val oppfrisker = NomCacheOppfrisker(nom, cfg)

    beforeEach {
        clearMocks(nom)
    }

    Given("en nomgraph cache-nøkkel") {
        When("id-en har AnsattId-format") {
            Then("oppfriskes ansatt med AnsattId") {
                oppfrisker.oppfrisk(CacheNøkkel("nomgraph::A123456"))
                verify(exactly = 1) { nom.ansatt(AnsattId("A123456")) }
            }
        }

        When("id-en har NomIdent-format") {
            Then("oppfriskes ansatteForNomIdent med NomIdent") {
                oppfrisker.oppfrisk(CacheNøkkel("nomgraph::ab123c"))
                verify(exactly = 1) { nom.ansatteForNomIdent(NomIdent("ab123c")) }
            }
        }

        When("id-en har Enhetnummer-format") {
            Then("oppfriskes ansatteForEnhet med Enhetnummer") {
                oppfrisker.oppfrisk(CacheNøkkel("nomgraph::0315"))
                verify(exactly = 1) { nom.ansatteForEnhet(Enhetnummer("0315")) }
            }
        }
    }

    Given("en nomgraph cache-nøkkel med ugyldig id-format") {
        When("id-en ikke matcher noen av typene") {
            Then("oppfriskes ingenting") {
                oppfrisker.oppfrisk(CacheNøkkel("nomgraph::!ugyldig!"))
                verify { nom wasNot Called }
            }
        }
    }
})
