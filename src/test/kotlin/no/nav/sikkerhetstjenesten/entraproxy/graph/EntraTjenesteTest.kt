package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheInaktiveAnsatte
import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheInaktiveNavIdenter.Companion.INAKTIVE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import java.util.UUID.randomUUID

class EntraTjenesteTest : BehaviorSpec({
    val graph = mockk<EntraGraphOperations>()
    val oid = mockk<EntraOidTjeneste>()
    val cache = mockk<CacheOperations>()
    val enheter = mockk<EnhetOperations>()
    val tjeneste = EntraTjeneste(graph, CacheInaktiveAnsatte(cache), EntraOidOppslag(oid, cache), enheter)
    val ident = AnsattId("A123456")
    val gammelOid = randomUUID()
    val nyOid = randomUUID()
    val temaer = setOf(Tema("AAP"))

    beforeEach {
        clearMocks(graph, oid, cache, enheter)
        every { cache.inneholder(INAKTIVE, ident.verdi) } returns false
    }

    Given("et Graph-oppslag med en utdatert OID") {
        beforeEach {
            every { oid.ansattOid(ident) } returnsMany listOf(gammelOid, nyOid)
            every { graph.temaer(gammelOid) } throws EntraIkkeFunnetException(ident)
            every { cache.delete(OID_CACHE, ident.verdi) } returns true
        }

        When("oppslaget lykkes med oppfrisket OID") {
            Then("slettes cacheinnslaget før nytt OID-oppslag og nytt Graph-kall") {
                every { graph.temaer(nyOid) } returns temaer

                val resultat = tjeneste.tema(ident)

                assertSoftly {
                    resultat shouldBe temaer
                    verifySequence {
                        cache.inneholder(INAKTIVE, ident.verdi)
                        oid.ansattOid(ident)
                        graph.temaer(gammelOid)
                        cache.delete(OID_CACHE, ident.verdi)
                        oid.ansattOid(ident)
                        graph.temaer(nyOid)
                    }
                }
            }
        }

        When("heller ikke oppfriskingen finner en OID") {
            Then("kastes EntraIkkeFunnetException uten et nytt Graph-kall") {
                every { oid.ansattOid(ident) } returnsMany listOf(gammelOid, null)

                val feil = shouldThrow<EntraIkkeFunnetException> { tjeneste.tema(ident) }

                assertSoftly {
                    feil.ansattId shouldBe ident
                    verify(exactly = 1) { cache.delete(OID_CACHE, ident.verdi) }
                    verify(exactly = 2) { oid.ansattOid(ident) }
                    verify(exactly = 1) { graph.temaer(any()) }
                }
            }
        }

        When("Graph-oppslaget feiler også etter oppfrisking") {
            Then("propageres feilen uten flere oppfriskingsforsøk") {
                val forventetFeil = EntraIkkeFunnetException(ident, "Fant heller ikke oppfrisket OID")
                every { graph.temaer(nyOid) } throws forventetFeil

                val feil = shouldThrow<EntraIkkeFunnetException> { tjeneste.tema(ident) }

                assertSoftly {
                    feil shouldBe forventetFeil
                    verify(exactly = 1) { cache.delete(OID_CACHE, ident.verdi) }
                    verify(exactly = 2) { oid.ansattOid(ident) }
                    verify(exactly = 1) { graph.temaer(gammelOid) }
                    verify(exactly = 1) { graph.temaer(nyOid) }
                }
            }
        }
    }

    Given("et vanlig OID-oppslag") {
        When("Graph kaster en annen feil") {
            Then("propageres feilen uten å slette eller oppfriske OID") {
                val forventetFeil = IllegalStateException("Graph er utilgjengelig")
                every { oid.ansattOid(ident) } returns gammelOid
                every { graph.temaer(gammelOid) } throws forventetFeil

                val feil = shouldThrow<IllegalStateException> { tjeneste.tema(ident) }

                assertSoftly {
                    feil shouldBe forventetFeil
                    verify(exactly = 1) { oid.ansattOid(ident) }
                    verify(exactly = 1) { graph.temaer(gammelOid) }
                    verify(exactly = 0) { cache.delete(any(), any()) }
                }
            }
        }

        When("ingen OID finnes") {
            Then("returneres et tomt sett uten Graph-kall") {
                every { oid.ansattOid(ident) } returns null

                val resultat = tjeneste.tema(ident)

                assertSoftly {
                    resultat shouldBe emptySet()
                    verify { graph wasNot Called }
                    verify(exactly = 0) { cache.delete(any(), any()) }
                }
            }
        }
    }

    Given("en inaktiv ansatt") {
        beforeEach {
            every { cache.inneholder(INAKTIVE, ident.verdi) } returns true
        }

        When("temaer hentes") {
            Then("returneres et tomt sett uten OID- eller Graph-oppslag") {
                val resultat = tjeneste.tema(ident)

                assertSoftly {
                    resultat shouldBe emptySet()
                    verify(exactly = 1) { cache.inneholder(INAKTIVE, ident.verdi) }
                    verify { listOf(graph, oid) wasNot Called }
                }
            }
        }

        When("enheter hentes") {
            Then("returneres et tomt sett uten OID-, Graph- eller enhetsnavnoppslag") {
                val resultat = tjeneste.enheter(ident)

                assertSoftly {
                    resultat shouldBe emptySet()
                    verify(exactly = 1) { cache.inneholder(INAKTIVE, ident.verdi) }
                    verify { listOf(graph, oid, enheter) wasNot Called }
                }
            }
        }
    }

    Given("søk etter matchende grupper") {
        When("nøyaktig 100 grupper matcher") {
            Then("hentes medlemmer for alle gruppene") {
                val grupper = (1..100).map { EntraGruppe("ROLLE_$it", randomUUID()) }.toSet()
                val medlemmer = setOf(Ansatt(ident, "Test Ansatt", "Test", "Ansatt"))
                every { graph.grupperSomMatcher("ROLLE") } returns grupper
                every { graph.gruppeMedlemmer(any()) } returns medlemmer

                val resultat = tjeneste.medlemmerIGrupperSomMatcher("ROLLE")

                assertSoftly {
                    resultat shouldBe grupper.map { GruppeMedMedlemmer(it.gruppeId!!, it.rolle, medlemmer) }
                    grupper.forEach {
                        verify(exactly = 1) { graph.gruppeMedlemmer(it.gruppeId.toString()) }
                    }
                }
            }
        }

        When("101 grupper matcher") {
            Then("avvises søket før medlemsoppslag starter") {
                every { graph.grupperSomMatcher("ROLLE") } returns
                    (1..101).map { EntraGruppe("ROLLE_$it", randomUUID()) }.toSet()

                val feil = shouldThrow<ForMangeMatchendeGrupperException> {
                    tjeneste.medlemmerIGrupperSomMatcher("ROLLE")
                }

                assertSoftly {
                    feil.gruppenavnSubstring shouldBe "ROLLE"
                    feil.antall shouldBe 101
                    feil.maks shouldBe 100
                    verify(exactly = 0) { graph.gruppeMedlemmer(any()) }
                }
            }
        }

        When("ett av de parallelle medlemsoppslagene feiler") {
            Then("propageres den opprinnelige feilen uten å returnere et delvis resultat") {
                val gruppe1 = EntraGruppe("ROLLE_1", randomUUID())
                val gruppe2 = EntraGruppe("ROLLE_2", randomUUID())
                val forventetFeil = IllegalStateException("Medlemsoppslag feilet")
                every { graph.grupperSomMatcher("ROLLE") } returns setOf(gruppe1, gruppe2)
                every { graph.gruppeMedlemmer(gruppe1.gruppeId.toString()) } returns
                    setOf(Ansatt(ident, "Test Ansatt", "Test", "Ansatt"))
                every { graph.gruppeMedlemmer(gruppe2.gruppeId.toString()) } throws forventetFeil

                val feil = shouldThrow<IllegalStateException> {
                    tjeneste.medlemmerIGrupperSomMatcher("ROLLE")
                }

                assertSoftly {
                    feil shouldBe forventetFeil
                    verify(exactly = 1) { graph.gruppeMedlemmer(gruppe1.gruppeId.toString()) }
                    verify(exactly = 1) { graph.gruppeMedlemmer(gruppe2.gruppeId.toString()) }
                }
            }
        }

        When("ingen grupper matcher") {
            Then("returneres en tom liste uten medlemsoppslag") {
                every { graph.grupperSomMatcher("ROLLE") } returns emptySet()

                val resultat = tjeneste.medlemmerIGrupperSomMatcher("ROLLE")

                assertSoftly {
                    resultat shouldBe emptyList()
                    verify(exactly = 0) { graph.gruppeMedlemmer(any()) }
                }
            }
        }
    }
})
