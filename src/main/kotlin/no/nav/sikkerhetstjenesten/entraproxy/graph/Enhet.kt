package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.requireDigits


data class Enhet(val enhetnummer: Enhetnummer, val navn: String = UKJENT) : Comparable<Enhet> {

    override fun compareTo(other: Enhet): Int = enhetnummer.compareTo(other.enhetnummer)

    data class Enhetnummer(private val nummer: String) : Comparable<Enhetnummer> {

        @JsonValue
        val verdi = nummer.removePrefix(ENHET_PREFIX)

        init {
            requireDigits(verdi,4)
        }
        val gruppeNavn = "${ENHET_PREFIX}$verdi"

        override fun compareTo(other: Enhetnummer): Int = verdi.compareTo(other.verdi)

        companion object {
            private const val ENHETNUMMER_LENGTH = 4

            fun parse(nummer: String): Enhetnummer? =
                runCatching {
                    Enhetnummer(nummer)
                }.getOrNull()
        }
    }
    companion object {
        const val ENHET_PREFIX = "0000-GA-ENHET_"
    }
}

