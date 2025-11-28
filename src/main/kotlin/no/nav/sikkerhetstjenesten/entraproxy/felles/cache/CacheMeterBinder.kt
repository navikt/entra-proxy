package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component

@Component
class CacheMeterBinder(private val client: CacheClient) : MeterBinder {

    override fun bindTo(registry: MeterRegistry) {
        client.cfgs.forEach { cfg ->
            registry.gauge("cache.size", Tags.of("navn", cfg.navn), client) { handler ->
                client.cacheStørrelse(cfg.navn)
            }
        }
    }
}