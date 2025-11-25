package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.Requirements.requireDigits

data class TIdent (@JsonValue val verdi: String) : Comparable<TIdent> {
    init {
        with(verdi) {
            require(length == ANSATTID_LENGTH) { "Ugyldig lengde $length for $this, forventet $ANSATTID_LENGTH" }
            require(first().isLetter()) { "Ugyldig første tegn ${first()} i $this, må være stor bokstav" }
            requireDigits(substring(3), 4)
        }
    }

    override fun compareTo(other: TIdent): Int = verdi.compareTo(other.verdi)

    companion object {
        const val ANSATTID_LENGTH = 7
    }

    override fun toString() = verdi
}