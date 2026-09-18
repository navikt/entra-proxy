package no.nav.sikkerhetstjenesten.entraproxy.felles.utils


import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LederUtvelger.LeaderChangedEvent
import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.event.EventListener
import java.net.InetAddress.getLocalHost

abstract class LeaderAware(private var erLeder: Boolean = false) {
    private val hostname = getLocalHost().hostName
    protected open fun doHandleLeaderChange() = Unit

    private val log = getLogger(javaClass)

    @EventListener(LeaderChangedEvent::class)
    fun onApplicationEvent(event: LeaderChangedEvent) {
        erLeder = event.leder == hostname
        somLeder("håndtering av lederbytte", {
            log.info("Denne instansen ($hostname) er nå leder")
            doHandleLeaderChange()
        }) { log.info("Denne instansen ($hostname) er ikke leder, lederen er ${event.leder}") }
    }

    protected fun somLeder(beskrivelse: String? = null, block: () -> Unit) =
        somLeder(beskrivelse, block) {}


    protected fun <T> somLeder(beskrivelse: String? = null, block: () -> T, default: () -> T): T =
        if (erLeder) {
            beskrivelse?.let { log.trace(it) }
            block()
        } else {
            default()
        }
}