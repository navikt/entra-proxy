package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.Enhetnummer


data class Enhet(val enhetnummer: Enhetnummer, val navn: String) : Comparable<Enhet> {

    override fun compareTo(other: Enhet): Int = enhetnummer.compareTo(other.enhetnummer)

    companion object {
        const val ENHET_PREFIX = "0000-GA-ENHET_"
    }
}

