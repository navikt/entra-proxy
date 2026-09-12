package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import java.time.Duration

interface CachableRestConfig {
    val varighet: Duration get() = Duration.ofHours(12)
    val navn: String
    val cacheNulls: Boolean get() = false
    val caches: Set<CacheNøkkelConfig>
}