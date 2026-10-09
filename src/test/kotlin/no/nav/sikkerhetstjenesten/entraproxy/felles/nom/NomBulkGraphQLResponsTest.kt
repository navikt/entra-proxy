package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class NomBulkGraphQLResponsTest : BehaviorSpec({
    val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    Given("a bulk response element from NOM") {
        When("ressurs is missing") {
            Then("the element is decoded without a resource") {
                mapper.readValue("{}", NomBulkGraphQLRespons::class.java).ressurs shouldBe null
            }
        }

        When("ressurs is null") {
            Then("the element is decoded without a resource") {
                mapper.readValue("""{"ressurs":null}""", NomBulkGraphQLRespons::class.java).ressurs shouldBe null
            }
        }

        When("ressurs is present") {
            Then("the resource is preserved") {
                val respons = mapper.readValue(
                    """{"ressurs":{"navident":"A123456","visningsnavn":"First","gjeldendeSektor":"STAT"}}""",
                    NomBulkGraphQLRespons::class.java
                )
                respons.ressurs?.navident?.verdi shouldBe "A123456"
            }
        }
    }
})
