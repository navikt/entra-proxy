package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule.Builder

private data class OrgEnheterHolder(val orgEnheter: List<NomOrgEnhetRespons> = emptyList())

class NomOrgEnhetResponsTest : BehaviorSpec({
    val mapper = JsonMapper.builder().addModule(Builder().build()).build()

    Given("an orgEnheter response from NOM with ansatte") {
        val json = NomOrgEnhetResponsTest::class.java
            .getResource("/nom/orgenheter-med-ansatte.json")!!.readText()
        val enheter = mapper.readValue(json, OrgEnheterHolder::class.java).orgEnheter

        When("the response is decoded") {
            Then("every org unit is preserved in order") {
                enheter.map { it.orgEnhet?.id?.verdi } shouldContainExactly
                    listOf("fa515j", "ke465u", "de122m", "de150v", "hy893h")
            }

            Then("the first org unit keeps its name and ansatte") {
                val forste = enheter.first().orgEnhet!!
                forste.navn shouldBe "Arbeidsgiverseksjonen"
                forste.orgTilknytninger.size shouldBe 17
                val ansatt = forste.orgTilknytninger.first().ressurs!!
                ansatt.navident.verdi shouldBe "H176430"
                ansatt.visningsnavn shouldBe "Anita Haugen"
                ansatt.gjeldendeSektor shouldBe "NAV_STATLIG"
            }

            Then("an org unit with a single ansatt is decoded") {
                val enhet = enheter.single { it.orgEnhet?.id?.verdi == "de150v" }.orgEnhet!!
                enhet.navn shouldBe "Avdeling for mennesker og organisasjon"
                enhet.orgTilknytninger.single().ressurs!!.visningsnavn shouldBe "Linn Huse-Amundsen"
            }

            Then("all ansatte across every org unit are decoded with unique identities") {
                val ansatte = enheter.flatMap { it.orgEnhet?.orgTilknytninger.orEmpty() }
                    .mapNotNull { it.ressurs }
                ansatte.size shouldBe 71
                ansatte.map { it.navident.verdi }.distinct().size shouldBe 71
            }

            Then("names with special characters are preserved") {
                val navn = enheter.flatMap { it.orgEnhet?.orgTilknytninger.orEmpty() }
                    .mapNotNull { it.ressurs?.visningsnavn }
                navn.contains("Øivin Køber Opstad") shouldBe true
                navn.contains("Sølvi Agnete Minde") shouldBe true
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
                    """{"orgEnhet":{"id":"fa515j","navn":"Arbeidsgiverseksjonen"}}""",
                    NomOrgEnhetRespons::class.java
                )
                respons.orgEnhet!!.orgTilknytninger shouldBe emptyList()
            }
        }
    }
})
