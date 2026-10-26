package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.oppfriskere.EntraOidCacheOppfrisker
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkel
import java.time.Duration.ofDays

class EntraOidCacheOppfriskerTest : BehaviorSpec({

    val cfg = EntraOidConfig(ofDays(365))
    val oid = mockk<EntraOidTjeneste>(relaxed = true)
    val oppfrisker = EntraOidCacheOppfrisker(oid, cfg)

    beforeEach {
        clearMocks(oid)
    }

    Given("en ENTRA_OID cache-nøkkel") {
        When("id-en har AnsattId-format") {
            Then("oppfriskes kun ansattOid med AnsattId") {
                oppfrisker.oppfrisk(CacheNøkkel("entraoid::A123456"))
                assertSoftly {
                    verify(exactly = 1) {
                        oid.ansattOid(AnsattId("A123456"))
                    }
                    verify(exactly = 0) {
                        oid.gruppeOid(any())
                    }
                }
            }
        }

        When("id-en er et gruppenavn") {
            Then("oppfriskes kun gruppeOid med gruppenavnet") {
                oppfrisker.oppfrisk(CacheNøkkel("entraoid::En gruppe"))
                assertSoftly {
                    verify(exactly = 1) {
                        oid.gruppeOid("En gruppe")
                    }
                    verify(exactly = 0) {
                        oid.ansattOid(any())
                    }
                }
            }
        }
    }

    Given("et OID-oppslag som feiler") {
        When("ansattoppslaget kaster unntak") {
            Then("håndteres feilen uten å forsøke et gruppeoppslag") {
                every { oid.ansattOid(any()) } throws IllegalStateException("Entra er utilgjengelig")

                oppfrisker.oppfrisk(CacheNøkkel("entraoid::A123456"))
                assertSoftly {
                    verify(exactly = 1) {
                        oid.ansattOid(AnsattId("A123456"))
                    }
                    verify(exactly = 0) {
                        oid.gruppeOid(any())
                    }
                }
            }
        }

        When("gruppeoppslaget kaster unntak") {
            Then("håndteres feilen uten å forsøke et ansattoppslag") {
                every { oid.gruppeOid(any()) } throws IllegalStateException("Entra er utilgjengelig")

                oppfrisker.oppfrisk(CacheNøkkel("entraoid::En gruppe"))
                assertSoftly {
                    verify(exactly = 1) {
                        oid.gruppeOid("En gruppe")
                    }
                    verify(exactly = 0) {
                        oid.ansattOid(any())
                    }
                }
            }
        }
    }
})
