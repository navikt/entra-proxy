package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LeaderAware
import org.springframework.stereotype.Component

@Component
class CacheStørrelseMeterBinder(private val client: CacheClient) :  LeaderAware(), MeterBinder {

    override fun bindTo(registry: MeterRegistry)   {
        somLeder(0L) {
            client.cfgs.forEach { cfg ->
                registry.gauge("cache.size", Tags.of("navn", cfg.navn), client) { handler ->
                    client.cacheStørrelse(cfg.navn)
                }
            }
        }
    }
}