package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class NomOrgTilknytningTest : BehaviorSpec({
    val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    Given("an org unit from NOM") {
        When("tilgangsenhetId is missing") {
            Then("the org unit is decoded without an access unit") {
                val enhet = mapper.readValue(
                    """{"id":"ra656d","navn":"Testenhet"}""",
                    NomOrgTilknytning.NomEnhet::class.java
                )
                enhet.tilgangsenhetId shouldBe null
            }
        }

        When("tilgangsenhetId is null") {
            Then("the org unit is decoded without an access unit") {
                val enhet = mapper.readValue(
                    """{"id":"ra656d","navn":"Testenhet","tilgangsenhetId":null}""",
                    NomOrgTilknytning.NomEnhet::class.java
                )
                enhet.tilgangsenhetId shouldBe null
            }
        }

        When("tilgangsenhetId is present") {
            Then("the access unit is preserved") {
                val enhet = mapper.readValue(
                    """{"id":"ra656d","navn":"Testenhet","tilgangsenhetId":"1234"}""",
                    NomOrgTilknytning.NomEnhet::class.java
                )
                enhet.tilgangsenhetId shouldBe Enhetnummer("1234")
            }
        }
    }
})
