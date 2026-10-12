package no.nav.sikkerhetstjenesten.entraproxy.norg

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

class NorgTjenesteTest : BehaviorSpec({
    val norg = mockk<NorgPort>()
    val tjeneste = NorgTjeneste(norg)
    val enhetsnummer = Enhetnummer("0315")

    Given("a unit name from the port") {
        When("looking up the name for a unit") {
            every { norg.navnFor(enhetsnummer) } returns "NAV Testenhet"

            Then("the service returns the name from the port") {
                tjeneste.navnFor(enhetsnummer) shouldBe "NAV Testenhet"
            }
        }
    }
})
