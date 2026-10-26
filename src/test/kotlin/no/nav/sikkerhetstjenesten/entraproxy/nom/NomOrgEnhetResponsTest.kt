package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLRespons.Sektor.NAV_STATLIG
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule.Builder

private data class OrgEnheterHolder(val orgEnheter: List<NomOrgEnhetRespons> = emptyList())

class NomOrgEnhetResponsTest : BehaviorSpec({
    val mapper = JsonMapper.builder().addModule(Builder().build()).build()

    Given("an orgEnheter response from NOM with ansatte") {
        val json = NomOrgEnhetResponsTest::class.java
            .getResource("/nom/orgenheter-med-ansatte.json")!!.readText()

        When("the response is decoded") {
            Then("the org unit and its employee fields are preserved") {
                val enheter = mapper.readValue(json, OrgEnheterHolder::class.java).orgEnheter
                val enhet = enheter.first().orgEnhet!!
                val ansatt = enhet.orgTilknytninger.first().ressurs!!

                assertSoftly {
                    enhet.id.verdi shouldBe "ab123c"
                    enhet.navn shouldBe "Arbeidsgiverseksjonen"
                    ansatt.navident.verdi shouldBe "H123456"
                    ansatt.visningsnavn shouldBe "Gaus Dahl"
                    ansatt.gjeldendeSektor shouldBe NAV_STATLIG
                }
            }
        }
    }

    Given("a single orgEnheter element") {
        When("orgEnhet is missing") {
            Then("the element is decoded without an org unit") {
                mapper.readValue("{}", NomOrgEnhetRespons::class.java).orgEnhet shouldBe null
            }
        }

        When("the org unit has no orgTilknytninger") {
            Then("the ansatte list is empty") {
                val respons = mapper.readValue(
                    """{"orgEnhet":{"id":"ab123c","navn":"Arbeidsgiverseksjonen"}}""",
                    NomOrgEnhetRespons::class.java
                )
                respons.orgEnhet!!.orgTilknytninger shouldBe emptyList()
            }
        }
    }
})
