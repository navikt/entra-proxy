package no.nav.sikkerhetstjenesten.entraproxy.cache

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifySequence
import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheInaktiveNavIdenter.Companion.INAKTIVE
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphOperations
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import java.util.UUID.randomUUID

class CacheInaktiveNavIdenterTest : BehaviorSpec({
    val graph = mockk<EntraGraphOperations>()
    val cache = mockk<CacheOperations>()
    val gruppeId = randomUUID()
    val oppdaterer = CacheInaktiveNavIdenter(graph, cache, gruppeId)

    beforeEach {
        clearMocks(graph, cache)
    }

    Given("periodisk oppdatering av inaktive ansatte") {
        When("Graph returnerer medlemmer i gruppen") {
            Then("erstattes cachesettet med medlemmenes navidenter") {
                val identer = setOf("A123456", "B123456")
                every { graph.gruppeMedlemmer(gruppeId.toString()) } returns identer.mapTo(mutableSetOf()) {
                    Ansatt(AnsattId(it), "Test Ansatt", "Test", "Ansatt")
                }
                every { cache.putSet(INAKTIVE, identer) } just runs
                every { cache.getSet(INAKTIVE) } returns identer

                oppdaterer.oppdaterCache()

                verifySequence {
                    graph.gruppeMedlemmer(gruppeId.toString())
                    cache.putSet(INAKTIVE, identer)
                    cache.getSet(INAKTIVE)
                }
            }
        }

        When("gruppen er tom") {
            Then("tømmes cachesettet slik at gamle identer ikke beholdes") {
                every { graph.gruppeMedlemmer(gruppeId.toString()) } returns emptySet()
                every { cache.putSet(INAKTIVE, emptySet()) } just runs
                every { cache.getSet(INAKTIVE) } returns emptySet()

                oppdaterer.oppdaterCache()

                verifySequence {
                    graph.gruppeMedlemmer(gruppeId.toString())
                    cache.putSet(INAKTIVE, emptySet())
                    cache.getSet(INAKTIVE)
                }
            }
        }

        When("Graph-oppslaget feiler") {
            Then("beholdes cacheinnholdet uten at feilen kastes videre") {
                every { graph.gruppeMedlemmer(gruppeId.toString()) } throws IllegalStateException("Graph er utilgjengelig")

                shouldNotThrowAny { oppdaterer.oppdaterCache() }

                verify(exactly = 1) { graph.gruppeMedlemmer(gruppeId.toString()) }
                verify { cache wasNot Called }
            }
        }

        When("lagring i cache feiler") {
            Then("avbrytes oppdateringen uten at feilen kastes videre") {
                every { graph.gruppeMedlemmer(gruppeId.toString()) } returns emptySet()
                every { cache.putSet(INAKTIVE, emptySet()) } throws IllegalStateException("Cache er utilgjengelig")

                shouldNotThrowAny { oppdaterer.oppdaterCache() }

                verifySequence {
                    graph.gruppeMedlemmer(gruppeId.toString())
                    cache.putSet(INAKTIVE, emptySet())
                }
            }
        }
    }
})
