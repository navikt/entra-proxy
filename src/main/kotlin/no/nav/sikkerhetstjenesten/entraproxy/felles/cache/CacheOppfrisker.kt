package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH

interface CacheOppfrisker {
     val cacheName: String get() = GRAPH
    fun oppfrisk(nøkkelElementer: CacheNøkkelElementer) = Unit
}