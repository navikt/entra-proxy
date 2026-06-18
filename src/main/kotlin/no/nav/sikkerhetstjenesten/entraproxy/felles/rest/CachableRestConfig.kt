package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import java.time.Duration

interface CachableRestConfig {
    val varighet: Duration
    val navn: String
    val caches: Set<CacheNøkkelConfig>
}

