package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.springframework.graphql.client.GraphQlClient

class NomSyncGraphQLClientAdapterTest : BehaviorSpec({
    Given("bulk queries") {
        val client = mockk<GraphQlClient>()
        val request = mockk<GraphQlClient.RequestSpec>()
        val retrieve = mockk<GraphQlClient.RetrieveSyncSpec>()
        val adapter = NomSyncGraphQLClientAdapter(NomGraphQLConfig("localhost"), client)

        every { client.documentName("orgtilknytninger-bulk") } returns request
        every { request.variables(mapOf("navidenter" to listOf("A123456", "B123456"))) } returns request
        every { request.retrieveSync("ressurser") } returns retrieve

        When("the response contains resources") {
            val first = NomGraphQLRespons(AnsattId("A123456"), "First", "NAV")
            val second = NomGraphQLRespons(AnsattId("B123456"), "Second", "NAV")
            every { retrieve.toEntityList(NomBulkGraphQLRespons::class.java) } returns
                listOf(NomBulkGraphQLRespons(first), NomBulkGraphQLRespons(second))

            Then("list variables are sent and resources are unwrapped") {
                adapter.orgDataBulk("A123456", "B123456") shouldBe listOf(first, second)
                verify { request.variables(mapOf("navidenter" to listOf("A123456", "B123456"))) }
                verify { request.retrieveSync("ressurser") }
            }
        }

        When("the response contains an empty resource list") {
            every { retrieve.toEntityList(NomBulkGraphQLRespons::class.java) } returns emptyList()

            Then("an empty list is returned") {
                adapter.orgDataBulk("A123456", "B123456") shouldBe emptyList()
            }
        }

        When("the query fails") {
            val failure = IllegalStateException("GraphQL query failed")
            every { request.retrieveSync("ressurser") } throws failure

            Then("the failure is propagated") {
                shouldThrow<IllegalStateException> {
                    adapter.orgDataBulk("A123456", "B123456")
                } shouldBe failure
            }
        }
    }
})
