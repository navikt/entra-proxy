package no.nav.sikkerhetstjenesten.entraproxy.felles.utils


import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LederUtvelger.NyLederHendelse
import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.event.EventListener
import java.net.InetAddress.getLocalHost

abstract class LeaderAware(private var erLeder: Boolean = false) {
    private val hostname = getLocalHost().hostName
    protected open fun doHandleLeaderChange(nyLeder: String) = Unit

    private val log = getLogger(javaClass)

    @EventListener(NyLederHendelse::class)
    open fun onApplicationEvent(event: NyLederHendelse) {
        erLeder = event.leder == hostname
        log.info("Denne instansen er $hostname, lederen er ${event.leder}")
        doHandleLeaderChange(event.leder)
    }

    protected fun somLeder(beskrivelse: String? = null, block: () -> Unit) {
        if (erLeder) {
            beskrivelse?.let { log.trace(it) }
            block()
        }
    }
}