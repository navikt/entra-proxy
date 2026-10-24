package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOM_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel

class NomCacheOppfriskerTest : BehaviorSpec({

    Given("en nomgraph cache-nøkkel") {
        When("id-en har AnsattId-format") {
            val nom = mockk<NomTjeneste>(relaxed = true)
            val cfg = mockk<NomGraphQLConfig> {
                every { caches } returns setOf(NOM_CACHE)
            }
            NomCacheOppfrisker(nom,cfg).oppfrisk(CacheNøkkel("nomgraph::A123456"))

            Then("oppfriskes ansatt med AnsattId") {
                verify(exactly = 1) { nom.ansatt(AnsattId("A123456")) }
            }
        }

        When("id-en har NomIdent-format") {
            val nom = mockk<NomTjeneste>(relaxed = true)
            val cfg = mockk<NomGraphQLConfig> {
                every { caches } returns setOf(NOM_CACHE)
            }
            NomCacheOppfrisker(nom,cfg).oppfrisk(CacheNøkkel("nomgraph::ab123c"))

            Then("oppfriskes ansatteForNomIdent med NomIdent") {
                verify(exactly = 1) { nom.ansatteForNomIdent(NomIdent("ab123c")) }
            }
        }

        When("id-en har Enhetnummer-format") {
            val nom = mockk<NomTjeneste>(relaxed = true)
            val cfg = mockk<NomGraphQLConfig> {
                every { caches } returns setOf(NOM_CACHE)
            }
            NomCacheOppfrisker(nom,cfg).oppfrisk(CacheNøkkel("nomgraph::0315"))

            Then("oppfriskes ansatteForEnhet med Enhetnummer") {
                verify(exactly = 1) { nom.ansatteForEnhet(Enhetnummer("0315")) }
            }
        }
    }

    Given("en nomgraph cache-nøkkel med ugyldig id-format") {
        When("id-en ikke matcher noen av typene") {
            val nom = mockk<NomTjeneste>(relaxed = true)
            val cfg = mockk<NomGraphQLConfig> {
                every { caches } returns setOf(NOM_CACHE)
            }
            NomCacheOppfrisker(nom,cfg).oppfrisk(CacheNøkkel("nomgraph::!ugyldig!"))

            Then("oppfriskes ingenting") {
                verify { nom wasNot Called }
            }
        }
    }
})
