package no.nav.sikkerhetstjenesten.entraproxy.felles.utils


import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import java.net.InetAddress

abstract class LeaderAware(var erLeder: Boolean = false) {
    private val hostname = InetAddress.getLocalHost().hostName
    protected fun doHandleLeaderChange()  = Unit

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(LederUtvelger.LeaderChangedEvent::class)
    fun onApplicationEvent(event: LederUtvelger.LeaderChangedEvent) {
        erLeder = event.leder == hostname
        if (erLeder) {
            log.trace("Denne instansen ($hostname) er nå leder")
            doHandleLeaderChange()
        }
        else {
            log.trace("Denne instansen ($hostname) er IKKE leder, lederen er ${event.leder}")
        }
    }
}