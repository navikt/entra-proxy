package no.nav.sikkerhetstjenesten.entraproxy.graph

data class EntraGruppe(val verdi: String): Comparable<EntraGruppe> {
    override fun compareTo(other: EntraGruppe): Int =verdi.compareTo(other.verdi)
}