package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component

@Component
class CacheStørrelseMeterBinder(private val client: CacheClient) :   MeterBinder {
    private val log = getLogger(javaClass)

    override fun bindTo(registry: MeterRegistry)   {
        log.info("Binding to registry for cache størrelser")
        client.cfgs.forEach { cfg ->
            registry.gauge("cache.size", Tags.of("navn", cfg.navn), client) { _ ->
                client.cacheStørrelse(cfg.navn).toDouble().also {
                    log.info("Cache størrelse for cache '${cfg.navn}': $it innslag" )
                }
            }
        }
    }
}