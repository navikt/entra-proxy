package no.nav.sikkerhetstjenesten.entraproxy.felles.utils


import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LederUtvelger.LeaderChangedEvent
import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.event.EventListener
import java.net.InetAddress

abstract class LeaderAware {
    private val hostname = InetAddress.getLocalHost().hostName
    protected fun doHandleLeaderChange()  = Unit

    private var erLeder: Boolean = false
    private val log = getLogger(javaClass)

    @EventListener(LeaderChangedEvent::class)
    fun onApplicationEvent(event: LeaderChangedEvent) {
        erLeder = event.leder == hostname
        if (erLeder) {
            log.info("Denne instansen ($hostname) er nå leder")
            doHandleLeaderChange()
        }
    }

    protected fun <T> somLeder(default: T, beskrivelse : String,block: () -> T): T = if (erLeder) {
       log.trace("Kjører $beskrivelse som leder")
        block()
    } else {
        log.trace("Kjører ikke $beskrivelse som leder, returnerer default")
        default
    }
}