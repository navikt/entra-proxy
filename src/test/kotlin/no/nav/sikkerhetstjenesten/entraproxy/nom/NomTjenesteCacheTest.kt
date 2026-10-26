package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootTest(classes = [NomTjenesteCacheTest.CacheTestConfig::class], webEnvironment = NONE)
class NomTjenesteCacheTest(
    private val tjeneste: NomTjeneste,
    private val nom: NomOperations
) : BehaviorSpec() {

    @Configuration
    @EnableCaching
    class CacheTestConfig {
        @Bean
        fun cacheManager() = CaffeineCacheManager(NOMGRAPH)

        @Bean
        fun nomPort(): NomOperations = mockk()

        @Bean
        fun nomTjeneste(nom: NomOperations) = NomTjeneste(nom)
    }

    init {
        val ident = AnsattId("A123456")
        val enhet = Enhetnummer("0315")
        val ansatt = NomAnsatt(ident, "Test Ansatt", "STAT")

        Given("en NomTjeneste med caching og en mocket port") {

            When("samme ident slås opp to ganger") {
                every { nom.ansatt(ident) } returns ansatt

                Then("kalles porten bare én gang") {
                    val first = tjeneste.ansatt(ident)
                    val second = tjeneste.ansatt(ident)

                    assertSoftly {
                        first shouldBe ansatt
                        second shouldBe ansatt
                        verify(exactly = 1) { nom.ansatt(ident) }
                    }
                }
            }

            When("samme tilgangsenhet slås opp to ganger") {
                every { nom.ansatteForEnhet(enhet) } returns setOf(ansatt)

                Then("kalles porten bare én gang") {
                    val first = tjeneste.ansatteForEnhet(enhet)
                    val second = tjeneste.ansatteForEnhet(enhet)

                    assertSoftly {
                        first shouldBe setOf(ansatt)
                        second shouldBe setOf(ansatt)
                        verify(exactly = 1) { nom.ansatteForEnhet(enhet) }
                    }
                }
            }

            When("samme sett med identer slås opp to ganger") {
                every { nom.ansatte(setOf(ident)) } returns setOf(ansatt)

                Then("kalles porten hver gang fordi oppslag av flere identer ikke caches") {
                    val first = tjeneste.ansatte(setOf(ident))
                    val second = tjeneste.ansatte(setOf(ident))

                    assertSoftly {
                        first shouldBe setOf(ansatt)
                        second shouldBe setOf(ansatt)
                        verify(exactly = 2) { nom.ansatte(setOf(ident)) }
                    }
                }

            }

            When("samme NOM-ident slås opp to ganger") {
                Then("returneres de ansatte og porten kalles bare én gang") {
                    val nomIdent = NomIdent("fa515j")
                    val enhetAnsatte = NomEnhetAnsatte(nomIdent, "Toppenhet", setOf(ansatt))
                    every { nom.ansatteForNomIdent(nomIdent) } returns enhetAnsatte

                    val first = tjeneste.ansatteForNomIdent(nomIdent)
                    val second = tjeneste.ansatteForNomIdent(nomIdent)

                    assertSoftly {
                        first shouldBe enhetAnsatte
                        second shouldBe enhetAnsatte
                        verify(exactly = 1) { nom.ansatteForNomIdent(nomIdent) }
                    }
                }
            }
        }
    }
}
