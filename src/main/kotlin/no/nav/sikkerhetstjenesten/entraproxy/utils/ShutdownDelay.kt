package no.nav.sikkerhetstjenesten.entraproxy.utils

import java.time.Duration
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.availability.AvailabilityChangeEvent.publish
import org.springframework.boot.availability.ReadinessState.REFUSING_TRAFFIC
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.SmartLifecycle
import org.springframework.context.SmartLifecycle.DEFAULT_PHASE
import org.springframework.stereotype.Component

/**
 * Venter ved SIGTERM før webserveren slutter å ta imot forbindelser, slik at
 * pod'en rekker å bli fjernet fra endpoints (og Wonderwall slutter å sende trafikk).
 * Stoppes før WebServerGracefulShutdownLifecycle (fase DEFAULT_PHASE - 1024).
 */
@Component
class ShutdownDelay(
    @Value($$"${server.shutdown-delay:0s}") private val delay: Duration,
    private val publisher: ApplicationEventPublisher,
) : SmartLifecycle {

    private val log = getLogger(javaClass)
    private var running = false

    override fun start() {
        log.info("Starter")
        running = true
    }

    override fun stop() {
        running = false
        if (!delay.isZero) {
            log.info("Markerer readiness som REFUSING_TRAFFIC og venter {} før nedstenging", delay)
            publish(publisher, this, REFUSING_TRAFFIC)
            Thread.sleep(delay)
        }
    }

    override fun isRunning() = running

    override fun getPhase() = DEFAULT_PHASE
}
