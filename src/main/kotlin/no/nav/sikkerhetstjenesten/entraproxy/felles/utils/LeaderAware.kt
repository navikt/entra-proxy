package no.nav.sikkerhetstjenesten.entraproxy.felles.utils


import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LederUtvelger.LeaderChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import java.net.InetAddress

abstract class LeaderAware(var erLeder: Boolean = false) {
    private val hostname = InetAddress.getLocalHost().hostName
    protected fun doHandleLeaderChange()  = Unit

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(LeaderChangedEvent::class)
    fun onApplicationEvent(event: LeaderChangedEvent) {
        erLeder = event.leder == hostname
        if (erLeder) {
            log.info("Denne instansen ($hostname) er nå leder")
            doHandleLeaderChange()
        }
    }
}