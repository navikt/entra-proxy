package no.nav.sikkerhetstjenesten.entraproxy.felles.leder


import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.event.EventListener
import java.net.InetAddress.getLocalHost

abstract class LeaderAware(private var erLeder: Boolean = false) {
    private val log = getLogger(javaClass)

    @EventListener(LederHendelse::class)
    open fun onApplicationEvent(hendelse: LederHendelse) {
        erLeder = hendelse.leder == HOSTNAME
        log.trace("Denne instansen er $HOSTNAME, lederen er ${hendelse.leder}")
    }

    protected fun <T> somLeder(beskrivelse: String? = null, block: () -> T): T? {
        if (erLeder) {
            beskrivelse?.let { log.trace(it) }
            return block()
        }
        return null
    }
    companion object {
        private val HOSTNAME = getLocalHost().hostName
    }
}