package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

data class CacheNøkkel(val verdi: String) {
    private val elementer = verdi.split("::", ":")
    val cacheName = elementer.first()
    val metode = if (elementer.size > 2) elementer[1] else null
    val id = elementer.last()

    override fun toString() = "CacheNøkkel(verdi='$verdi', cacheName='$cacheName', metode=$metode, id='$id')"
}