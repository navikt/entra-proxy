package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere.MedlemmerCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import java.util.UUID

class MedlemmerCacheOppfriskerTest : BehaviorSpec({

    Given("en MEDLEMMER cache-nøkkel") {
        When("id-en er en gyldig UUID") {
            val entra = mockk<EntraTjeneste>(relaxed = true)
            val cfg = mockk<MedlemmerConfig> {
                every { caches } returns setOf(MEDLEMMER_CACHE)
            }
            val gruppeId = UUID.randomUUID()
            MedlemmerCacheOppfrisker(entra,cfg).oppfrisk(CacheNøkkel("medlemmer::$gruppeId"))

            Then("oppfriskes medlemmerIGruppe med gruppe-id-en") {
                verify(exactly = 1) { entra.medlemmerIGruppe(gruppeId) }
            }
        }

        When("id-en ikke er en gyldig UUID") {
            val entra = mockk<EntraTjeneste>(relaxed = true)
            val cfg = mockk<MedlemmerConfig> {
                every { caches } returns setOf(MEDLEMMER_CACHE)
            }
            MedlemmerCacheOppfrisker(entra,cfg).oppfrisk(CacheNøkkel("medlemmer::ikke-en-uuid"))

            Then("oppfriskes ingenting") {
                verify { entra wasNot Called }
            }
        }
    }
})
