package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import no.nav.sikkerhetstjenesten.entraproxy.felles.NoCoverageAnalysis

data class CacheNøkkel(val verdi: String) {
    private val elementer = verdi.split("::", ":")
    val cacheName = elementer.first()
    val metode = if (elementer.size > 2) elementer[1] else null
    val id = elementer.last()

    @NoCoverageAnalysis
    override fun toString() = "${javaClass.simpleName} [nøkkel=$verdi]"
}