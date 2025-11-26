package no.nav.sikkerhetstjenesten.entraproxy.graph

data class EntraGruppe(val rolle: String): Comparable<EntraGruppe> {
    override fun compareTo(other: EntraGruppe): Int =rolle.compareTo(other.rolle)
}