package no.nav.sikkerhetstjenesten.entraproxy.utils

import java.time.Duration
import org.slf4j.LoggerFactory
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.context.SmartLifecycle.DEFAULT_PHASE
import org.springframework.stereotype.Component

/**
 * Venter ved SIGTERM før webserveren slutter å ta imot forbindelser, slik at
 * pod'en rekker å bli fjernet fra endpoints (og Wonderwall slutter å sende trafikk).
 * Stoppes før WebServerGracefulShutdownLifecycle (fase DEFAULT_PHASE - 1024).
 */
@Component
class ShutdownDelay(@Value($$"${server.shutdown-delay:0s}") private val delay: Duration) : SmartLifecycle {

    private val log = getLogger(javaClass)
    private var running = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
        if (!delay.isZero) {
            log.info("Venter {} før nedstenging", delay)
            Thread.sleep(delay)
        }
    }

    override fun isRunning() = running

    override fun getPhase() = DEFAULT_PHASE
}
