package no.nav.sikkerhetstjenesten.entraproxy.felles

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.slf4j.LoggerFactory.getLogger

class ParallelleOppslagTest : BehaviorSpec({
    val logger = getLogger("no.nav.sikkerhetstjenesten.entraproxy.felles.ParallelleOppslag") as Logger
    val opprinneligNivaa = logger.level
    val appender = ListAppender<ILoggingEvent>()

    beforeEach {
        appender.list.clear()
        appender.start()
        logger.level = Level.INFO
        logger.addAppender(appender)
    }

    afterEach {
        logger.detachAppender(appender)
        logger.level = opprinneligNivaa
        appender.stop()
    }

    Given("logging av ventetid for parallelle oppslag") {
        When("semaforen har et ledig permit") {
            Then("logges ikke et umiddelbart oppslag") {
                hentParallelt(listOf(1), 1) { it } shouldBe listOf(1)

                appender.list shouldBe emptyList()
            }
        }

        When("et oppslag venter over hundre millisekunder") {
            Then("logges bare ventetiden og samtidighetsgrensen") {
                hentParallelt(listOf(1, 2), 1) {
                    Thread.sleep(200)
                    it
                } shouldBe listOf(1, 2)

                val hendelse = appender.list.single()
                val ventetidMs = hendelse.argumentArray[0] as Long
                ventetidMs shouldBeGreaterThanOrEqual 100L
                hendelse.level shouldBe Level.INFO
                hendelse.formattedMessage shouldBe
                    "Ventet $ventetidMs ms på semafor (maks 1 samtidige oppslag)"
            }
        }
    }
})
