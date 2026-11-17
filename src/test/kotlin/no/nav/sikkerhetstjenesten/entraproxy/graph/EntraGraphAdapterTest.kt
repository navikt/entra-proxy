package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.BatchRequest
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.BatchRespons
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.BatchSubRespons
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.EntraGraphAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.client.EntraGraphClient
import no.nav.sikkerhetstjenesten.entraproxy.graph.exceptions.GrupperIkkeFunnetException
import org.springframework.web.client.RestClientResponseException
import java.util.UUID.randomUUID

class EntraGraphAdapterTest : BehaviorSpec({
    Given("validering av grupper i Entra") {
        When("alle gruppene finnes") {
            Then("valideres unike grupper i batcher på maksimalt 20") {
                val client = mockk<EntraGraphClient>()
                val gruppeIder = List(25) { randomUUID() }
                val forespørsler = mutableListOf<BatchRequest>()
                every { client.batch(capture(forespørsler)) } answers {
                    BatchRespons(firstArg<BatchRequest>().requests.reversed().map {
                        BatchSubRespons(it.id, 200)
                    })
                }

                EntraGraphAdapter(client).validerGrupper(*(gruppeIder + gruppeIder.first()).toTypedArray())

                forespørsler.map { it.requests.size } shouldBe listOf(20, 5)
                forespørsler.flatMap { it.requests }.map { it.url } shouldBe
                    gruppeIder.map { $$"/groups/$$it?$select=id" }
                forespørsler.flatMap { it.requests }.all { it.method == "GET" } shouldBe true
            }
        }

        When("listen er tom") {
            Then("gjøres ingen oppslag") {
                val client = mockk<EntraGraphClient>()

                EntraGraphAdapter(client).validerGrupper()

                verify(exactly = 0) { client.batch(any()) }
            }
        }

        When("en gruppe ikke finnes") {
            Then("kastes en exception med gruppe-id") {
                val client = mockk<EntraGraphClient>()
                val gruppeIder = List(2) { randomUUID() }
                every { client.batch(any()) } returns BatchRespons(
                    listOf(BatchSubRespons("1", 404), BatchSubRespons("0", 200))
                )

                val feil = shouldThrow<GrupperIkkeFunnetException> {
                    EntraGraphAdapter(client).validerGrupper(*gruppeIder.toTypedArray())
                }

                feil.gruppeIder shouldBe setOf(gruppeIder[1])
                feil.message shouldContain gruppeIder[1].toString()
            }
        }

        When("flere grupper ikke finnes") {
            Then("samles alle manglende grupper fra alle batcher uten duplikater") {
                val client = mockk<EntraGraphClient>()
                val gruppeIder = List(45) { randomUUID() }
                val manglendeGrupper = listOf(gruppeIder[0], gruppeIder[3], gruppeIder[20], gruppeIder[44])
                every { client.batch(any()) } answers {
                    BatchRespons(firstArg<BatchRequest>().requests.reversed().map { forespørsel ->
                        val status = if (manglendeGrupper.any {
                            forespørsel.url == $$"/groups/$$it?$select=id"
                        }) 404 else 200
                        BatchSubRespons(forespørsel.id, status)
                    })
                }

                val feil = shouldThrow<GrupperIkkeFunnetException> {
                    EntraGraphAdapter(client).validerGrupper(*(gruppeIder + manglendeGrupper).toTypedArray())
                }

                feil.gruppeIder shouldBe manglendeGrupper.toSet()
                manglendeGrupper.forEach { feil.message shouldContain it.toString() }
                verify(exactly = 3) { client.batch(any()) }
            }
        }

        When("en delforespørsel feiler av andre grunner") {
            Then("beholdes feilstatusen") {
                listOf(403, 429, 500).forEach { status ->
                    val client = mockk<EntraGraphClient>()
                    every { client.batch(any()) } returns BatchRespons(
                        listOf(BatchSubRespons("0", 404), BatchSubRespons("1", status))
                    )

                    shouldThrow<RestClientResponseException> {
                        EntraGraphAdapter(client).validerGrupper(randomUUID(), randomUUID())
                    }.statusCode.value() shouldBe status
                }
            }
        }

        When("en batchrespons mangler") {
            Then("feiler valideringen i stedet for å godkjenne gruppen") {
                val client = mockk<EntraGraphClient>()
                val gruppeId = randomUUID()
                every { client.batch(any()) } returns BatchRespons()

                shouldThrow<IllegalStateException> {
                    EntraGraphAdapter(client).validerGrupper(gruppeId)
                }.message shouldContain gruppeId.toString()
            }
        }

        When("batchkallet feiler") {
            Then("kastes feilen videre") {
                val client = mockk<EntraGraphClient>()
                val feil = IllegalStateException("Graph er utilgjengelig")
                every { client.batch(any()) } throws feil

                shouldThrow<IllegalStateException> {
                    EntraGraphAdapter(client).validerGrupper(randomUUID())
                } shouldBe feil
            }
        }
    }
})
