package no.nav.sikkerhetstjenesten.entraproxy.felles.leder


import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.event.EventListener
import java.net.InetAddress.getLocalHost

abstract class LeaderAware(private var erLeder: Boolean = false) {
    private val hostname = getLocalHost().hostName
    private val log = getLogger(javaClass)

    @EventListener(LederHendelse::class)
    open fun onApplicationEvent(event: LederHendelse) {
        erLeder = event.leder == hostname
        log.info("Denne instansen er $hostname, lederen er ${event.leder}")
    }

    protected fun somLeder(beskrivelse: String? = null, block: () -> Unit) {
        if (erLeder) {
            beskrivelse?.let { log.trace(it) }
            block()
        }
    }
}