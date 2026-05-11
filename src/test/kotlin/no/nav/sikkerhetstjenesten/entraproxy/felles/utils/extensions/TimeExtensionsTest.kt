package no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.format
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.local
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeExtensionsTest : BehaviorSpec({

    Given("Duration.format") {
        When("varighet er kun sekunder") {
            Then("singular brukes for 1 sekund") {
                1.seconds.format() shouldBe "1 sekund"
            }
            Then("plural brukes for flere sekunder") {
                30.seconds.format() shouldBe "30 sekunder"
            }
        }
        When("varighet er kombinasjon") {
            val s = (2.days + 3.hours + 4.minutes + 5.seconds).format()
            Then("inneholder dager") { s shouldContain "2 dager" }
            Then("inneholder timer") { s shouldContain "3 timer" }
            Then("inneholder minutter") { s shouldContain "4 minutter" }
            Then("inneholder sekunder") { s shouldContain "5 sekunder" }
        }
        When("varighet er 1 dag/time/minutt") {
            val s = (1.days + 1.hours + 1.minutes + 1.seconds).format()
            Then("bruker singular dag") { s shouldContain "1 dag " }
            Then("bruker singular time") { s shouldContain "1 time " }
            Then("bruker singular minutt") { s shouldContain "1 minutt " }
            Then("bruker singular sekund") { s shouldContain "1 sekund" }
        }
        When("varighet er null") {
            Then("returnerer tom string") {
                0.seconds.format() shouldBe ""
            }
        }
        When("varighet mangler en del") {
            val s = (2.days + 5.seconds).format()
            Then("inneholder kun ikke-null deler") {
                s shouldContain "2 dager"
                s shouldContain "5 sekunder"
                s shouldNotContain "time"
                s shouldNotContain "minutt"
            }
        }
    }

    Given("java.time.Duration.format") {
        When("brukt pa java.time.Duration") {
            Then("returnerer samme som Kotlin Duration") {
                java.time.Duration.ofMinutes(5).format() shouldBe "5 minutter"
            }
        }
    }

    Given("Long.local") {
        When("epoch millis konverteres med standard format") {
            Then("har formatet yyyy-MM-dd HH:mm:ss") {
                val formatted = 0L.local()
                formatted.length shouldBe "yyyy-MM-dd HH:mm:ss".length
            }
        }
        When("egendefinert format gis") {
            Then("bruker formatet") {
                val s = 0L.local("yyyy")
                s.length shouldBe 4
            }
        }
    }
})

