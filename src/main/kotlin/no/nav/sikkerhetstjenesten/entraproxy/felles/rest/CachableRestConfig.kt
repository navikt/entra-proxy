package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import no.nav.sikkerhetstjenesten.entraproxy.felles.NoCoverageAnalysis
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import java.time.Duration
import java.time.Duration.ofHours

@NoCoverageAnalysis
interface CachableRestConfig {
    val varighet: Duration get() = ofHours(12)
    val navn: String
    val cacheNulls: Boolean get() = false
    val caches: Set<CacheNøkkelConfig>
}

