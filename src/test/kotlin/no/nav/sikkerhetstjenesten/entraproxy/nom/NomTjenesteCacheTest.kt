package no.nav.sikkerhetstjenesten.entraproxy.nom

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomTjenesteCacheTest.CacheTestConfig
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootTest(classes = [CacheTestConfig::class], webEnvironment = NONE)
class NomTjenesteCacheTest(
    private val tjeneste: NomTjeneste,
    private val nom: NomPort
) : BehaviorSpec() {

    @Configuration
    @EnableCaching
    class CacheTestConfig {
        @Bean
        fun cacheManager() = CaffeineCacheManager(NOMGRAPH)

        @Bean
        fun nomPort(): NomPort = mockk()

        @Bean
        fun nomTjeneste(nom: NomPort) = NomTjeneste(nom)
    }

    init {
        val ident = AnsattId("A123456")
        val enhet = Enhetnummer("0315")
        val ansatt = NomAnsatt(ident, "Test Ansatt", "STAT")

        Given("a cached NomTjeneste backed by a mocked port") {

            When("the same ident is looked up twice") {
                every { nom.ansatt(ident) } returns ansatt

                Then("the port is queried only once") {
                    tjeneste.orgData(ident) shouldBe ansatt
                    tjeneste.orgData(ident) shouldBe ansatt
                    verify(exactly = 1) { nom.ansatt(ident) }
                }
            }

            When("the same tilgangsenhet is looked up twice") {
                every { nom.ansatteForEnhet(enhet) } returns setOf(ansatt)

                Then("the port is queried only once") {
                    tjeneste.ansatteForEnhet(enhet) shouldBe setOf(ansatt)
                    tjeneste.ansatteForEnhet(enhet) shouldBe setOf(ansatt)
                    verify(exactly = 1) { nom.ansatteForEnhet(enhet) }
                }
            }

            When("the same bulk of idents is looked up twice") {
                every { nom.ansatte(setOf(ident)) } returns setOf(ansatt)

                Then("the port is queried every time because bulk lookups are not cached") {
                    tjeneste.orgDataBulk(setOf(ident)) shouldBe setOf(ansatt)
                    tjeneste.orgDataBulk(setOf(ident)) shouldBe setOf(ansatt)
                    verify(exactly = 2) { nom.ansatte(setOf(ident)) }
                }
            }
        }
    }
}
