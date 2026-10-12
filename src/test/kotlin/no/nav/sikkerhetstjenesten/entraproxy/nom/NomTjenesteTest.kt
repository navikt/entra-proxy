package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule.Builder

class NomTjenesteTest : BehaviorSpec({
    val nom = mockk<NomPort>()
    val tjeneste = NomTjeneste(nom)
    val ident = AnsattId("A123456")
    val enhetsnummer = Enhetnummer("0315")
    val tilknytninger = setOf(
        NomOrgTilknytning(
            NomEnhet(
                NomIdent("ra656d"),
                "Testenhet",
                enhetsnummer
            )
        )
    )
    val respons = NomGraphQLRespons(ident, "Test Ansatt", "STAT", tilknytninger)
    val ansatt = NomAnsatt(ident, "Test Ansatt", "STAT", tilknytninger)

    Given("employee data from the port") {
        When("looking up one employee") {
            every { nom.ansatt(ident) } returns ansatt

            Then("the service returns a transport-independent employee") {
                tjeneste.orgData(ident) shouldBe ansatt
            }
        }

        When("looking up employees in bulk") {
            every { nom.ansatte(setOf(ident)) } returns setOf(ansatt)

            Then("the service returns the employee data") {
                tjeneste.orgDataBulk(setOf(ident)) shouldBe setOf(ansatt)
            }
        }

        When("looking up employees for an access unit") {
            every { nom.ansatteForTilgangsenhet(enhetsnummer) } returns setOf(ansatt)

            Then("the service returns the employee data") {
                tjeneste.ansatteForTilgangsenhet(enhetsnummer) shouldBe setOf(ansatt)
            }
        }

        When("the result is empty") {
            every { nom.ansatte(emptySet()) } returns emptySet()
            every { nom.ansatteForTilgangsenhet(Enhetnummer("1234")) } returns emptySet()

            Then("the service preserves empty results") {
                tjeneste.orgDataBulk(emptySet()) shouldBe emptySet()
                tjeneste.ansatteForTilgangsenhet(Enhetnummer("1234")) shouldBe emptySet()
            }
        }

        When("serializing the employee for the existing pages") {
            val mapper = JsonMapper.builder().addModule(Builder().build()).build()

            Then("the JSON structure is unchanged") {
                mapper.readTree(mapper.writeValueAsString(ansatt)) shouldBe mapper.readTree(
                    """{
                        "navident": "A123456",
                        "visningsnavn": "Test Ansatt",
                        "gjeldendeSektor": "STAT",
                        "orgTilknytninger": [{
                            "orgEnhet": {
                                "id": "ra656d",
                                "navn": "Testenhet",
                                "tilgangsenhetId": "0315"
                            }
                        }]
                    }"""
                )
                mapper.readTree(mapper.writeValueAsString(setOf(ansatt))) shouldBe
                    mapper.readTree(mapper.writeValueAsString(setOf(respons)))
            }
        }
    }
})
