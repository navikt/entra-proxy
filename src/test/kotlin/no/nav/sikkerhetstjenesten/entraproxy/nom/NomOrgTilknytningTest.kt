package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule.Builder

class NomOrgTilknytningTest : BehaviorSpec({
    val mapper = JsonMapper.builder().addModule(Builder().build()).build()

    Given("NomIdent.parse") {
        When("verdien er gyldig") {
            listOf("ra656d", "aa000a", "zz999z").forEach { verdi ->
                Then("returneres NomIdent for '$verdi'") {
                    NomIdent.parse(verdi) shouldBe NomIdent(verdi)
                }
            }
        }
        When("verdien er ugyldig") {
            listOf(
                "", "ra656", "ra656dd", "Ra656d", "ra656D", "1a656d", "ra65dd", "ra6561", " ra656d", "ra656d ",
            ).forEach { verdi ->
                Then("returneres null for '$verdi'") {
                    NomIdent.parse(verdi).shouldBeNull()
                }
            }
        }
    }

    Given("an org unit from NOM") {
        When("tilgangsenhetId is missing") {
            Then("the org unit is decoded without an access unit") {
                val enhet = mapper.readValue(
                    """{"id":"ra656d","navn":"Testenhet"}""",
                    NomEnhet::class.java
                )
                enhet.tilgangsenhetId shouldBe null
            }
        }

        When("tilgangsenhetId is null") {
            Then("the org unit is decoded without an access unit") {
                val enhet = mapper.readValue(
                    """{"id":"ra656d","navn":"Testenhet","tilgangsenhetId":null}""",
                    NomEnhet::class.java
                )
                enhet.tilgangsenhetId shouldBe null
            }
        }

        When("tilgangsenhetId is present") {
            Then("the access unit is preserved") {
                val enhet = mapper.readValue(
                    """{"id":"ra656d","navn":"Testenhet","tilgangsenhetId":"1234"}""",
                    NomEnhet::class.java
                )
                enhet.tilgangsenhetId shouldBe Enhetnummer("1234")
            }
        }
    }
})
