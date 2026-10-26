package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere.MedlemmerCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import java.time.Duration.ofHours
import java.util.UUID

class MedlemmerCacheOppfriskerTest : BehaviorSpec({

    val entra = mockk<EntraTjeneste>(relaxed = true)
    val oppfrisker = MedlemmerCacheOppfrisker(entra, MedlemmerConfig(ofHours(3)))

    beforeEach {
        clearMocks(entra)
    }

    Given("en MEDLEMMER cache-nøkkel") {
        When("id-en er en gyldig UUID") {
            Then("oppfriskes medlemmerIGruppe med gruppe-id-en") {
                val gruppeId = UUID.randomUUID()
                oppfrisker.oppfrisk(CacheNøkkel("medlemmer::$gruppeId"))

                verify(exactly = 1) {
                    entra.medlemmerIGruppe(gruppeId)
                }
            }
        }

        When("id-en ikke er en gyldig UUID") {
            Then("oppfriskes ingenting") {
                oppfrisker.oppfrisk(CacheNøkkel("medlemmer::ikke-en-uuid"))

                verify {
                    entra wasNot Called
                }
            }
        }
    }
})
