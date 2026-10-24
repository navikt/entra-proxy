package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere.EntraOidCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel

class EntraOidCacheOppfriskerTest : BehaviorSpec({

    Given("en ENTRA_OID cache-nøkkel") {
        When("id-en har AnsattId-format") {
            val oid = mockk<EntraOidTjeneste>(relaxed = true)
            val cfg = mockk<EntraOidConfig> {
                every { caches } returns setOf(OID_CACHE)
            }
            EntraOidCacheOppfrisker(oid,cfg).oppfrisk(CacheNøkkel("entraoid::A123456"))

            Then("oppfriskes kun ansattOid med AnsattId") {
                verify(exactly = 1) { oid.ansattOid(AnsattId("A123456")) }
                verify(exactly = 0) { oid.gruppeOid(any()) }
            }
        }

        When("id-en er et gruppenavn") {
            val oid = mockk<EntraOidTjeneste>(relaxed = true)
            val cfg = mockk<EntraOidConfig> {
                every { caches } returns setOf(OID_CACHE)
            }
            EntraOidCacheOppfrisker(oid,cfg).oppfrisk(CacheNøkkel("entraoid::En gruppe"))

            Then("oppfriskes kun gruppeOid med gruppenavnet") {
                verify(exactly = 1) { oid.gruppeOid("En gruppe") }
                verify(exactly = 0) { oid.ansattOid(any()) }
            }
        }
    }

    Given("et OID-oppslag som feiler") {
        When("ansattoppslaget kaster et unntak") {
            val oid = mockk<EntraOidTjeneste>(relaxed = true)
            val cfg = mockk<EntraOidConfig> {
                every { caches } returns setOf(OID_CACHE)
            }
            every { oid.ansattOid(any()) } throws IllegalStateException("Entra er utilgjengelig")

            Then("håndteres feilen uten å forsøke et gruppeoppslag") {
                EntraOidCacheOppfrisker(oid,cfg).oppfrisk(CacheNøkkel("entraoid::A123456"))

                verify(exactly = 1) { oid.ansattOid(AnsattId("A123456")) }
                verify(exactly = 0) { oid.gruppeOid(any()) }
            }
        }

        When("gruppeoppslaget kaster et unntak") {
            val oid = mockk<EntraOidTjeneste>(relaxed = true)
            val cfg = mockk<EntraOidConfig> {
                every { caches } returns setOf(OID_CACHE)
            }
            every { oid.gruppeOid(any()) } throws IllegalStateException("Entra er utilgjengelig")

            Then("håndteres feilen uten å forsøke et ansattoppslag") {
                EntraOidCacheOppfrisker(oid,cfg).oppfrisk(CacheNøkkel("entraoid::En gruppe"))

                verify(exactly = 1) { oid.gruppeOid("En gruppe") }
                verify(exactly = 0) { oid.ansattOid(any()) }
            }
        }
    }
})
