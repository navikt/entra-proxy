package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOM_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLRespons.Sektor.NAV_STATLIG
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import org.springframework.graphql.client.ClientGraphQlResponse
import org.springframework.graphql.client.ClientResponseField
import org.springframework.graphql.client.GraphQlClient
import org.springframework.graphql.client.GraphQlClient.RequestSpec
import org.springframework.graphql.client.GraphQlClient.RetrieveSyncSpec
import org.springframework.graphql.client.toEntityList

class NomSyncGraphQLClientAdapterTest : BehaviorSpec({
    val client = mockk<GraphQlClient>()
    val request = mockk<RequestSpec>()
    val retrieve = mockk<RetrieveSyncSpec>()
    val response = mockk<ClientGraphQlResponse>()
    val field = mockk<ClientResponseField>()
    val cache = mockk<CacheOperations>(relaxed = true)
    val adapter = NomSyncGraphQLClientAdapter(NomGraphQLConfig("localhost"), client, cache)
    val first = NomGraphQLRespons(AnsattId("A123456"), "Første ansatt", NAV_STATLIG)
    val second = NomGraphQLRespons(AnsattId("B123456"), "Andre ansatt", NAV_STATLIG)
    val firstAnsatt = NomAnsatt(AnsattId("A123456"), "Første ansatt", "Statlig")
    val secondAnsatt = NomAnsatt(AnsattId("B123456"), "Andre ansatt", "Statlig")
    val identer = setOf(first.navident, second.navident)
    val tilgangsenhetId = Enhetnummer("0315")
    val nomId = NomIdent("fa515j")

    fun dagligOppfolging(tilgangsenhet: Enhetnummer = tilgangsenhetId) = setOf(
        NomOrgTilknytning(NomEnhet(NomIdent("ra656d"), "Testenhet", tilgangsenhet), erDagligOppfolging = true)
    )

    fun enhetRespons(id: NomIdent, vararg tilknytninger: NomBulkGraphQLRespons) =
        NomOrgEnhetRespons(NomEnhetMedAnsatte(id, "Testenhet", tilknytninger.toList()))

    beforeEach {
        clearMocks(client, request, retrieve, response, field, cache)
    }

    Given("oppslag av én ansatt") {
        beforeEach {
            every { client.documentName("orgtilknytninger") } returns request
            every { request.variables(mapOf("navident" to "A123456")) } returns request
            every { request.executeSync() } returns response
            every { response.field("ressurs") } returns field
        }

        When("NOM returnerer en ansatt med flere tilknytninger") {
            Then("mappes den ansatte og bare daglig oppfølging beholdes") {
                val medOppfolging = dagligOppfolging()
                val utenOppfolging = NomOrgTilknytning(NomEnhet(NomIdent("xy123z"), "Annen enhet", tilgangsenhetId))
                every { field.toEntity(NomGraphQLRespons::class.java) } returns
                    first.copy(orgTilknytninger = medOppfolging + utenOppfolging)

                val resultat = adapter.ansatt(first.navident)

                assertSoftly {
                    resultat shouldBe firstAnsatt.copy(orgTilknytninger = medOppfolging)
                    verify(exactly = 1) { request.variables(mapOf("navident" to "A123456")) }
                    verify(exactly = 1) { response.field("ressurs") }
                }
            }
        }

        When("NOM ikke finner den ansatte") {
            Then("returneres null") {
                every { field.toEntity(NomGraphQLRespons::class.java) } returns null

                adapter.ansatt(first.navident) shouldBe null
            }
        }
    }

    Given("oppslag av flere ansatte") {
        beforeEach {
            every { client.documentName("orgtilknytninger-bulk") } returns request
            every { request.variables(mapOf("navidenter" to setOf("A123456", "B123456"))) } returns request
            every { request.retrieveSync("ressurser") } returns retrieve
        }

        When("alle identene finnes i cache") {
            Then("returneres alle ansatte fra cache uten oppslag i NOM") {
                every { cache.getMany(NOM_CACHE, setOf("A123456", "B123456"), NomAnsatt::class) } returns
                    mapOf("A123456" to firstAnsatt, "B123456" to secondAnsatt)

                val resultat = adapter.ansatte(identer)

                assertSoftly {
                    resultat shouldBe setOf(firstAnsatt, secondAnsatt)
                    verify { client wasNot Called }
                }
            }
        }

        When("bare noen av identene finnes i cache") {
            Then("hentes bare manglende ansatte fra NOM og kombineres med cachetreffene") {
                every { cache.getMany(NOM_CACHE, setOf("A123456", "B123456"), NomAnsatt::class) } returns
                    mapOf("A123456" to firstAnsatt)
                every { request.variables(mapOf("navidenter" to setOf("B123456"))) } returns request
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns listOf(NomBulkGraphQLRespons(second))

                val resultat = adapter.ansatte(identer)

                assertSoftly {
                    resultat shouldBe setOf(firstAnsatt, secondAnsatt)
                    verify(exactly = 1) { request.variables(mapOf("navidenter" to setOf("B123456"))) }
                    verify(exactly = 1) { cache.putMany(NOM_CACHE, mapOf("B123456" to secondAnsatt)) }
                }
            }
        }

        When("cache inneholder et eksplisitt null-innslag") {
            Then("hentes den manglende ansatte fra NOM og cache oppdateres") {
                every { cache.getMany(NOM_CACHE, setOf("A123456", "B123456"), NomAnsatt::class) } returns
                    mapOf("A123456" to firstAnsatt, "B123456" to null)
                every { request.variables(mapOf("navidenter" to setOf("B123456"))) } returns request
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns listOf(NomBulkGraphQLRespons(second))

                val resultat = adapter.ansatte(identer)

                assertSoftly {
                    resultat shouldBe setOf(firstAnsatt, secondAnsatt)
                    verify(exactly = 1) { request.variables(mapOf("navidenter" to setOf("B123456"))) }
                    verify(exactly = 1) { cache.putMany(NOM_CACHE, mapOf("B123456" to secondAnsatt)) }
                }
            }
        }

        When("responsen inneholder ressurser") {
            Then("sendes identlisten og bare tilknytninger med daglig oppfølging beholdes") {
                val medOppfolging = dagligOppfolging()
                val utenOppfolging = NomOrgTilknytning(NomEnhet(NomIdent("xy123z"), "Annen enhet", tilgangsenhetId))
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns listOf(
                    NomBulkGraphQLRespons(first.copy(orgTilknytninger = medOppfolging + utenOppfolging)),
                    NomBulkGraphQLRespons(second)
                )
                val forventet = setOf(firstAnsatt.copy(orgTilknytninger = medOppfolging), secondAnsatt)

                val resultat = adapter.ansatte(identer)

                assertSoftly {
                    resultat shouldBe forventet
                    verify(exactly = 1) { request.variables(mapOf("navidenter" to setOf("A123456", "B123456"))) }
                    verify(exactly = 1) { request.retrieveSync("ressurser") }
                    verify(exactly = 1) {
                        cache.putMany(NOM_CACHE, forventet.associateBy { it.navident.verdi })
                    }
                }
            }
        }

        When("responsen inneholder en tom ressursliste") {
            Then("returneres et tomt sett") {
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns emptyList()

                adapter.ansatte(identer) shouldBe emptySet()
            }
        }

        When("en ressurs mangler i responsen") {
            Then("returneres de tilgjengelige ressursene") {
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns
                    listOf(NomBulkGraphQLRespons(first), NomBulkGraphQLRespons(null))

                adapter.ansatte(identer) shouldBe setOf(firstAnsatt)
            }
        }

        When("alle ressursene mangler") {
            Then("returneres et tomt sett") {
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns
                    listOf(NomBulkGraphQLRespons(), NomBulkGraphQLRespons(null))

                adapter.ansatte(identer) shouldBe emptySet()
            }
        }

        When("oppslaget feiler") {
            Then("kastes feilen videre") {
                val failure = IllegalStateException("GraphQL-oppslag feilet")
                every { request.retrieveSync("ressurser") } throws failure

                shouldThrow<IllegalStateException> {
                    adapter.ansatte(identer)
                } shouldBe failure
            }
        }
    }

    Given("ansatte for tilgangsenhet") {
        beforeEach {
            every { client.documentName("ansatte-for-tilgangsenhet") } returns request
            every { request.variables(mapOf("tilgangsenhetId" to "0315")) } returns request
            every { request.retrieveSync("ressurser") } returns retrieve
        }

        When("tilgangsenheten har ressurser med daglig oppfølging") {
            Then("sendes tilgangsenhetId og de ansatte returneres") {
                val tilknytninger = dagligOppfolging()
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns listOf(
                    NomBulkGraphQLRespons(first.copy(orgTilknytninger = tilknytninger)),
                    NomBulkGraphQLRespons(second.copy(orgTilknytninger = tilknytninger))
                )

                val resultat = adapter.ansatteForEnhet(tilgangsenhetId)

                assertSoftly {
                    resultat shouldBe setOf(
                        firstAnsatt.copy(orgTilknytninger = tilknytninger),
                        secondAnsatt.copy(orgTilknytninger = tilknytninger)
                    )
                    verify(exactly = 1) { request.variables(mapOf("tilgangsenhetId" to "0315")) }
                    verify(exactly = 1) { request.retrieveSync("ressurser") }
                }
            }
        }

        When("ressurser mangler daglig oppfølging for tilgangsenheten") {
            Then("returneres bare ressurser med daglig oppfølging for tilgangsenheten") {
                val medOppfolging = dagligOppfolging()
                val utenOppfolging = medOppfolging.mapTo(mutableSetOf()) { it.copy(erDagligOppfolging = false) }
                val third = NomGraphQLRespons(
                    AnsattId("C123456"), "Tredje ansatt", NAV_STATLIG, dagligOppfolging(Enhetnummer("0316"))
                )
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns listOf(
                    NomBulkGraphQLRespons(first.copy(orgTilknytninger = medOppfolging)),
                    NomBulkGraphQLRespons(second.copy(orgTilknytninger = utenOppfolging)),
                    NomBulkGraphQLRespons(third)
                )

                adapter.ansatteForEnhet(tilgangsenhetId) shouldBe
                    setOf(firstAnsatt.copy(orgTilknytninger = medOppfolging))
            }
        }

        When("tilgangsenheten ikke har ressurser") {
            Then("returneres et tomt sett") {
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns emptyList()

                adapter.ansatteForEnhet(tilgangsenhetId) shouldBe emptySet()
            }
        }

        When("en ressurs mangler") {
            Then("returneres de tilgjengelige ressursene") {
                val tilknytninger = dagligOppfolging()
                every { retrieve.toEntityList<NomBulkGraphQLRespons>() } returns listOf(
                    NomBulkGraphQLRespons(first.copy(orgTilknytninger = tilknytninger)),
                    NomBulkGraphQLRespons(null)
                )

                adapter.ansatteForEnhet(tilgangsenhetId) shouldBe
                    setOf(firstAnsatt.copy(orgTilknytninger = tilknytninger))
            }
        }

        When("oppslaget feiler") {
            Then("kastes feilen videre") {
                val failure = IllegalStateException("GraphQL-oppslag feilet")
                every { request.retrieveSync("ressurser") } throws failure

                shouldThrow<IllegalStateException> {
                    adapter.ansatteForEnhet(tilgangsenhetId)
                } shouldBe failure
            }
        }
    }

    Given("ansatte for nomident") {
        beforeEach {
            every { client.documentName("ansatte-for-nomid") } returns request
            every { request.variables(mapOf("organisertUnderOrgEnhetId" to "fa515j")) } returns request
            every { request.retrieveSync("orgEnheter") } returns retrieve
        }

        When("organisasjonsenhetene inneholder ressurser med og uten daglig oppfølging") {
            Then("returneres bare ressurser med daglig oppfølging fra alle organisasjonsenhetene") {
                every { retrieve.toEntityList<NomOrgEnhetRespons>() } returns listOf(
                    enhetRespons(
                        nomId,
                        NomBulkGraphQLRespons(first, erDagligOppfolging = true),
                        NomBulkGraphQLRespons(NomGraphQLRespons(AnsattId("C123456"), "Tredje ansatt", NAV_STATLIG))
                    ),
                    enhetRespons(NomIdent("ke465u"), NomBulkGraphQLRespons(second, erDagligOppfolging = true))
                )

                val resultat = adapter.ansatteForNomIdent(nomId)

                assertSoftly {
                    resultat shouldBe setOf(firstAnsatt, secondAnsatt)
                    verify(exactly = 1) { request.variables(mapOf("organisertUnderOrgEnhetId" to "fa515j")) }
                    verify(exactly = 1) { request.retrieveSync("orgEnheter") }
                }
            }
        }

        When("ingen organisasjonsenheter returneres") {
            Then("returneres et tomt sett") {
                every { retrieve.toEntityList<NomOrgEnhetRespons>() } returns emptyList()

                adapter.ansatteForNomIdent(nomId) shouldBe emptySet()
            }
        }

        When("en organisasjonsenhet mangler eller en tilknytning mangler ressurs") {
            Then("returneres de tilgjengelige ressursene") {
                every { retrieve.toEntityList<NomOrgEnhetRespons>() } returns listOf(
                    enhetRespons(
                        nomId,
                        NomBulkGraphQLRespons(first, erDagligOppfolging = true),
                        NomBulkGraphQLRespons(null, erDagligOppfolging = true)
                    ),
                    enhetRespons(NomIdent("de150v")),
                    NomOrgEnhetRespons(null)
                )

                adapter.ansatteForNomIdent(nomId) shouldBe setOf(firstAnsatt)
            }
        }

        When("oppslaget feiler") {
            Then("kastes feilen videre") {
                val failure = IllegalStateException("GraphQL-oppslag feilet")
                every { request.retrieveSync("orgEnheter") } throws failure

                shouldThrow<IllegalStateException> {
                    adapter.ansatteForNomIdent(nomId)
                } shouldBe failure
            }
        }
    }
})
