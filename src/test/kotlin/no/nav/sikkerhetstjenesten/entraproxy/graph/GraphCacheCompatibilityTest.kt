package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class GraphCacheCompatibilityTest : BehaviorSpec({
    Given("typer som lagres i cache") {
        val typer = mapOf(
            Ansatt::class.java to "no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt",
            UtvidetAnsatt::class.java to "no.nav.sikkerhetstjenesten.entraproxy.graph.UtvidetAnsatt",
            AnsattId::class.java to "no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId",
            Enhet::class.java to "no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet",
            Enhet.Enhetnummer::class.java to "no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet\$Enhetnummer",
            EntraGruppe::class.java to "no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppe",
            TIdent::class.java to "no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent",
            Tema::class.java to "no.nav.sikkerhetstjenesten.entraproxy.graph.Tema",
        )

        Then("beholdes de fullstendige klassenavnene for eksisterende cacheverdier") {
            typer.forEach { (type, forventetNavn) ->
                type.name shouldBe forventetNavn
            }
        }
    }
})
