package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.Requirements.requireDigits


data class Enhet(val enhetnummer: Enhetnummer, val navn: String) : Comparable<Enhet> {

    override fun compareTo(other: Enhet): Int = enhetnummer.compareTo(other.enhetnummer)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Enhet) return false
        return enhetnummer == other.enhetnummer &&
                navn == other.navn
    }

    override fun hashCode() = enhetnummer.hashCode()

    class Enhetnummer(private val nummer: String) : Comparable<Enhetnummer> {

        @JsonValue
        val verdi = nummer.removePrefix(ENHET_PREFIX)

        init {
            requireDigits(verdi,4)
        }
        val gruppeNavn = "${ENHET_PREFIX}$verdi"

        override fun compareTo(other: Enhetnummer): Int = verdi.compareTo(other.verdi)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Enhetnummer) return false
            return nummer == other.nummer
        }

        override fun hashCode() = nummer.hashCode()

    }
    companion object {
        const val ENHET_PREFIX = "0000-GA-ENHET_"
    }
}

