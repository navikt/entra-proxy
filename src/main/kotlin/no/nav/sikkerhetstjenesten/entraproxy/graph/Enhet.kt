package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits


data class Enhet(val enhetnummer: Enhetnummer, val navn: String) : Comparable<Enhet> {

    override fun compareTo(other: Enhet): Int = enhetnummer.compareTo(other.enhetnummer)


    class Enhetnummer(nummer: String) : Comparable<Enhetnummer> {

        @JsonValue
        val verdi = nummer.removePrefix(ENHET_PREFIX)

        init {
            requireDigits(verdi,4)
        }
        val gruppeNavn = "${ENHET_PREFIX}$verdi"

        override fun compareTo(other: Enhetnummer): Int = verdi.compareTo(other.verdi)

    }
    companion object {
        const val ENHET_PREFIX = "0000-GA-ENHET_"
    }
}

