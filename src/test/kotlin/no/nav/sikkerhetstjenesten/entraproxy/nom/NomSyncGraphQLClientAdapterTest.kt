package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOM_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import org.springframework.graphql.client.GraphQlClient
import org.springframework.graphql.client.GraphQlClient.RequestSpec
import org.springframework.graphql.client.GraphQlClient.RetrieveSyncSpec
import org.springframework.graphql.client.toEntityList

class NomSyncGraphQLClientAdapterTest : BehaviorSpec({
    val identer = setOf(AnsattId("A123456"), AnsattId("B123456"))
    val tilgangsenhetId = Enhetnummer("0315")
    Given("bulk queries") {
        val client = mockk<GraphQlClient>()
        val request = mockk<RequestSpec>()
        val retrieve = mockk<RetrieveSyncSpec>()
        val cache = mockk<CacheOperations>(relaxed = true)
        val adapter = NomSyncGraphQLClientAdapter(NomGraphQLConfig("localhost"), client, cache)

        every { client.documentName("orgtilknytninger-bulk") } returns request
        every { request.variables(mapOf("navidenter" to setOf("A123456", "B123456"))) } returns request
        every { request.retrieveSync("ressurser") } returns retrieve
        When("all idents are found in cache") {
            val cachedA = NomAnsatt(AnsattId("A123456"), "Cached A", "STAT")
            val cachedB = NomAnsatt(AnsattId("B123456"), "Cached B", "STAT")
            val strengClient = mockk<GraphQlClient>()
            val cacheMedAlle = mockk<CacheOperations>(relaxed = true)
            val adapterMedAlle = NomSyncGraphQLClientAdapter(NomGraphQLConfig("localhost"), strengClient, cacheMedAlle)
            every { cacheMedAlle.getMany(NOM_CACHE, setOf("A123456", "B123456"), NomAnsatt::class) } returns
                mapOf("A123456" to cachedA, "B123456" to cachedB)

            Then("all employees are returned from cache without querying NOM") {
                adapterMedAlle.ansatte(identer) shouldBe setOf(cachedA, cachedB)
            }
        }

        When("only some idents are found in cache") {
            val cachedA = NomAnsatt(AnsattId("A123456"), "Cached A", "STAT")
            val lokalClient = mockk<GraphQlClient>()
            val lokalRequest = mockk<RequestSpec>()
            val lokalRetrieve = mockk<RetrieveSyncSpec>()
            val cacheMedTreff = mockk<CacheOperations>(relaxed = true)
            val adapterMedTreff = NomSyncGraphQLClientAdapter(NomGraphQLConfig("localhost"), lokalClient, cacheMedTreff)
            every { cacheMedTreff.getMany(NOM_CACHE, setOf("A123456", "B123456"), NomAnsatt::class) } returns
                mapOf("A123456" to cachedA)
            every { lokalClient.documentName("orgtilknytninger-bulk") } returns lokalRequest
            every { lokalRequest.variables(mapOf("navidenter" to setOf("B123456"))) } returns lokalRequest
            every { lokalRequest.retrieveSync("ressurser") } returns lokalRetrieve
            val second = NomGraphQLRespons(AnsattId("B123456"), "Second", "STAT")
            every { lokalRetrieve.toEntityList<NomBulkGraphQLRespons>() } returns listOf(NomBulkGraphQLRespons(second))

            Then("only the missing idents are fetched from NOM and merged with the cache hits") {
                val forventetB = NomAnsatt(second.navident, second.visningsnavn, second.gjeldendeSektor)
                adapterMedTreff.ansatte(identer) shouldBe setOf(cachedA, forventetB)
                verify { lokalRequest.variables(mapOf("navidenter" to setOf("B123456"))) }
                verify { cacheMedTreff.putMany(NOM_CACHE, mapOf("B123456" to forventetB)) }
            }
        }

        When("the response contains resources") {
            val tilknytninger = setOf(
                NomOrgTilknytning(
                    NomEnhet(
                        NomIdent("ra656d"),
                        "Testenhet",
                        tilgangsenhetId
                    )
                )
            )
            val first = NomGraphQLRespons(AnsattId("A123456"), "First", "STAT", tilknytninger)
            val second = NomGraphQLRespons(AnsattId("B123456"), "Second", "STAT")
            every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns
                listOf(NomBulkGraphQLRespons(first), NomBulkGraphQLRespons(second))

            Then("list variables are sent and resources are unwrapped") {
                adapter.ansatte(identer) shouldBe setOf(
                    NomAnsatt(first.navident, first.visningsnavn, first.gjeldendeSektor, tilknytninger),
                    NomAnsatt(second.navident, second.visningsnavn, second.gjeldendeSektor)
                )
                verify { request.variables(mapOf("navidenter" to setOf("A123456", "B123456"))) }
                verify { request.retrieveSync("ressurser") }
            }
        }

        When("the response contains an empty resource list") {
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns emptyList()

            Then("an empty list is returned") {
                adapter.ansatte(identer) shouldBe emptySet()
            }
        }

        When("the response contains a missing resource") {
            val first = NomGraphQLRespons(AnsattId("A123456"), "First", "STAT")
            every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns
                listOf(NomBulkGraphQLRespons(first), NomBulkGraphQLRespons(null))

            Then("available resources are returned") {
                adapter.ansatte(identer) shouldBe setOf(
                    NomAnsatt(first.navident, first.visningsnavn, first.gjeldendeSektor)
                )
            }
        }

        When("all resources are missing") {
            every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns
                listOf(NomBulkGraphQLRespons(), NomBulkGraphQLRespons(null))

            Then("an empty list is returned") {
                adapter.ansatte(identer) shouldBe emptySet()
            }
        }

        When("the query fails") {
            val failure = IllegalStateException("GraphQL query failed")
            every { request.retrieveSync("ressurser") } throws failure

            Then("the failure is propagated") {
                shouldThrow<IllegalStateException> {
                    adapter.ansatte(identer)
                } shouldBe failure
            }
        }
    }

    Given("ansatte for tilgangsenhet") {
        val client = mockk<GraphQlClient>()
        val request = mockk<RequestSpec>()
        val retrieve = mockk<RetrieveSyncSpec>()
        val cache = mockk<CacheOperations>(relaxed = true)
        val adapter = NomSyncGraphQLClientAdapter(NomGraphQLConfig("localhost"), client, cache)

        every { client.documentName("ansatte-for-tilgangsenhet") } returns request
        every { request.variables(mapOf("tilgangsenhetId" to "0315")) } returns request
        every { request.retrieveSync("ressurser") } returns retrieve

        When("the tilgangsenhet has resources") {
            val first = NomGraphQLRespons(AnsattId("A123456"), "First", "STAT")
            val second = NomGraphQLRespons(AnsattId("B123456"), "Second", "STAT")
            every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns
                listOf(NomBulkGraphQLRespons(first), NomBulkGraphQLRespons(second))

            Then("the tilgangsenhetId variable is sent and all employees are returned") {
                adapter.ansatteForEenhet(tilgangsenhetId) shouldBe setOf(
                    NomAnsatt(first.navident, first.visningsnavn, first.gjeldendeSektor),
                    NomAnsatt(second.navident, second.visningsnavn, second.gjeldendeSektor)
                )
                verify { request.variables(mapOf("tilgangsenhetId" to "0315")) }
                verify { request.retrieveSync("ressurser") }
            }
        }

        When("the tilgangsenhet has no resources") {
            every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns emptyList()

            Then("an empty set is returned") {
                adapter.ansatteForEenhet(tilgangsenhetId) shouldBe emptySet()
            }
        }

        When("a resource is missing") {
            val first = NomGraphQLRespons(AnsattId("A123456"), "First", "STAT")
            every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns
                listOf(NomBulkGraphQLRespons(first), NomBulkGraphQLRespons(null))

            Then("available resources are returned") {
                adapter.ansatteForEenhet(tilgangsenhetId) shouldBe setOf(
                    NomAnsatt(first.navident, first.visningsnavn, first.gjeldendeSektor)
                )
            }
        }

        When("the query fails") {
            val failure = IllegalStateException("GraphQL query failed")
            every { request.retrieveSync("ressurser") } throws failure

            Then("the failure is propagated") {
                shouldThrow<IllegalStateException> {
                    adapter.ansatteForEenhet(tilgangsenhetId)
                } shouldBe failure
            }
        }
    }
})
