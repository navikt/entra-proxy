package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LeaderAware
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component

@Component
class CacheStørrelseMeterBinder(private val client: CacheClient) :  LeaderAware(), MeterBinder {
    private val log = getLogger(javaClass)

    override fun bindTo(registry: MeterRegistry)   {
        somLeder(0L) {
            client.cfgs.forEach { cfg ->
                registry.gauge("cache.size", Tags.of("navn", cfg.navn), client) { _ ->
                    client.cacheStørrelse(cfg.navn).also {
                        log.info("Cache størrelse for cache '${cfg.navn}': $it innslag" )
                    }
                }
            }
        }
    }
}