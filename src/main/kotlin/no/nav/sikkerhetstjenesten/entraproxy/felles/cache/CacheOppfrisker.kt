package no.nav.sikkerhetstjenesten.entraproxy.felles.cache


interface CacheOppfrisker {
    val cacheName: String
    fun oppfrisk(nøkkelElementer: CacheNøkkelElementer)
}