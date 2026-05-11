package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.time.Duration

class EntraConfigTest : BehaviorSpec({

    val cfg = EntraConfig(varighet = Duration.ofMinutes(5))

    Given("EntraConfig URI-bygging") {
        When("userURI(navIdent)") {
            val uri = cfg.userURI("A123456").toString()
            Then("har /users path") { uri shouldContain "/users" }
            Then("har select=id") { uri shouldContain "\$select=id" }
            Then("har count=true") { uri shouldContain "\$count=true" }
            Then("har filter med navIdent") {
                uri shouldContain "onPremisesSamAccountName"
                uri shouldContain "A123456"
            }
        }
        When("gruppeURI(displayName)") {
            val uri = cfg.gruppeURI("min-gruppe").toString()
            Then("har /groups path") { uri shouldContain "/groups" }
            Then("har filter med displayName") {
                uri shouldContain "displayName"
                uri shouldContain "min-gruppe"
            }
        }
        When("temaURI(oid)") {
            val uri = cfg.temaURI("oid-123").toString()
            Then("har memberOf path med oid") {
                uri shouldContain "/users/oid-123/memberOf"
            }
            Then("har TEMA-prefix i filter") {
                uri shouldContain "0000-GA-TEMA_"
            }
        }
        When("enheterURI(oid)") {
            val uri = cfg.enheterURI("oid-456").toString()
            Then("har memberOf path med oid") {
                uri shouldContain "/users/oid-456/memberOf"
            }
            Then("har ENHET-prefix i filter") {
                uri shouldContain "0000-GA-ENHET_"
            }
        }
        When("ansatteGruppeURI(oid)") {
            val uri = cfg.ansatteGruppeURI("oid-789").toString()
            Then("har memberOf path") {
                uri shouldContain "/users/oid-789/memberOf"
            }
            Then("har securityEnabled-filter") {
                uri shouldContain "securityEnabled"
            }
        }
        When("gruppeMedlemmerURI(gruppeId)") {
            val uri = cfg.gruppeMedlemmerURI("gr-1").toString()
            Then("har medlemmer-path med gruppeId") {
                uri shouldContain "/groups/gr-1/members"
            }
            Then("har top satt") { uri shouldContain "\$top=" }
            Then("har count=true") { uri shouldContain "\$count=true" }
        }
        When("navIdentURI(ansattId)") {
            val uri = cfg.navIdentURI("A123456").toString()
            Then("har /users path") { uri shouldContain "/users" }
            Then("filtrerer pa onPremisesSamAccountName") {
                uri shouldContain "onPremisesSamAccountName"
                uri shouldContain "A123456"
            }
        }
        When("tIdentURI(ansattId)") {
            val uri = cfg.tIdentURI("T123456").toString()
            Then("har /users path") { uri shouldContain "/users" }
            Then("filtrerer pa jobTitle") {
                uri shouldContain "jobTitle"
                uri shouldContain "T123456"
            }
        }
    }

    Given("EntraConfig konstanter") {
        When("baseUri leses") {
            Then("starter med graph-host") {
                cfg.baseUri.toString() shouldStartWith "https://graph.microsoft.com"
            }
        }
        When("navn leses") {
            Then("er graph") { cfg.navn shouldBe "graph" }
        }
    }
})

