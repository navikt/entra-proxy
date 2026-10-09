package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.springframework.graphql.client.GraphQlClient
import org.springframework.graphql.client.toEntityList

class NomSyncGraphQLClientAdapterTest : BehaviorSpec({
    Given("bulk queries") {
        val client = mockk<GraphQlClient>()
        val request = mockk<GraphQlClient.RequestSpec>()
        val retrieve = mockk<GraphQlClient.RetrieveSyncSpec>()
        val adapter = NomSyncGraphQLClientAdapter(NomGraphQLConfig("localhost"), client)

        every { client.documentName("orgtilknytninger-bulk") } returns request
        every { request.variables(mapOf("navidenter" to listOf("A123456", "B123456"))) } returns request
        every { request.retrieveSync("ressurser") } returns retrieve


       
        When("the query fails") {
            val failure = IllegalStateException("GraphQL query failed")
            every { request.retrieveSync("ressurser") } throws failure

            Then("the failure is propagated") {
                shouldThrow<IllegalStateException> {
                    adapter.orgDataBulk(listOf("A123456", "B123456"))
                } shouldBe failure
            }
        }
    }
})
