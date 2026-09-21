package no.nav.sikkerhetstjenesten.entraproxy.felles.leder


import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.event.EventListener
import java.net.InetAddress.getLocalHost

abstract class LeaderAware(private var erLeder: Boolean = false) {
    private val log = getLogger(javaClass)

    @EventListener(LederHendelse::class)
    open fun onApplicationEvent(hendelse: LederHendelse) {
        erLeder = hendelse.leder == HOSTNAME
        log.info("Denne instansen er $HOSTNAME, lederen er ${hendelse.leder}")
    }

    protected fun somLeder(beskrivelse: String? = null, block: () -> Unit) {
        if (erLeder) {
            beskrivelse?.let { log.trace(it) }
            block()
        }
    }
    companion object {
        private val HOSTNAME = getLocalHost().hostName
    }
}