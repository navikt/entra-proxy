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

    Given("en orgEnheter-respons fra NOM med ansatte") {
        val json = NomOrgEnhetResponsTest::class.java
            .getResource("/nom/orgenheter-med-ansatte.json")!!.readText()

        When("responsen dekodes") {
            Then("beholdes feltene for organisasjonsenheten og den ansatte") {
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

    Given("ett orgEnheter-element") {
        When("orgEnhet mangler") {
            Then("dekodes elementet uten organisasjonsenhet") {
                mapper.readValue("{}", NomOrgEnhetRespons::class.java).orgEnhet shouldBe null
            }
        }

        When("organisasjonsenheten mangler orgTilknytninger") {
            Then("er ansattlisten tom") {
                val respons = mapper.readValue(
                    """{"orgEnhet":{"id":"ab123c","navn":"Arbeidsgiverseksjonen"}}""",
                    NomOrgEnhetRespons::class.java
                )
                respons.orgEnhet!!.orgTilknytninger shouldBe emptyList()
            }
        }
    }
})
