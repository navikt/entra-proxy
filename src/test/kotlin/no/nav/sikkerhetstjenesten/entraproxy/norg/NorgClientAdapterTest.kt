package no.nav.sikkerhetstjenesten.entraproxy.norg

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

class NorgClientAdapterTest : BehaviorSpec({
    val client = mockk<NorgProxyClient>()
    val adapter = NorgClientAdapter(client)
    val enhet = Enhetnummer("0315")

    Given("en tilgangsenhet") {
        When("Norg returnerer enhetsdata") {
            every { client.enhetFor("0315") } returns NorgEnhetRespons(315, "NAV Testenhet")

            Then("returneres det transportuavhengige enhetsnavnet") {
                adapter.navn(enhet) shouldBe "NAV Testenhet"
                verify { client.enhetFor("0315") }
            }
        }

        When("klienten feiler") {
            val failure = IllegalStateException("Norg-oppslag feilet")
            every { client.enhetFor("0315") } throws failure

            Then("kastes feilen videre") {
                shouldThrow<IllegalStateException> {
                    adapter.navn(enhet)
                } shouldBe failure
            }
        }
    }
})
