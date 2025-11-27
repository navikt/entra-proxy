package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.instrument.Tags.of
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LeaderAware
import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.SmartLifecycle
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component


@Component
class CacheExpiredEventListener(val teller: CacheOppfriskerTeller, erLeder: Boolean = false, private vararg val oppfriskere: CacheOppfrisker = emptyArray()) :
    LeaderAware(erLeder), SmartLifecycle {
    private val log = getLogger(javaClass)
    private var running = false
    @EventListener
    fun cacheInnslagFjernet(hendelse: CacheElementUtløptLytter.CacheInnslagFjernetHendelse) {
        somLeder {
            if (isRunning()) {
                val elementer = CacheNøkkelElementer(hendelse.nøkkel)
                log.info("Cache innslag utløpt for cache '${elementer.cacheName}' med nøkkel '${hendelse.nøkkel}'")
                oppfriskere.firstOrNull { it.cacheName == elementer.cacheName }?.run {
                    oppfrisk(elementer)
                    teller.tell(of("cache", elementer.cacheName, "result", "expired", "method", elementer.metode ?: "ingen"))
                }
            }
        }
    }

    override fun start() { running = true }
    override fun isRunning() = running
    override fun stop() { running = false }
}