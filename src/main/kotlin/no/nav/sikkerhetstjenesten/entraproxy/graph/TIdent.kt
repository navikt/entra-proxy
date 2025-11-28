package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.Requirements.requireDigits

data class TIdent (@JsonValue val verdi: String) {
    init {
        with(verdi) {
            require(length == TIDENT_LENGTH) { "Ugyldig lengde $length for $this, forventet $TIDENT_LENGTH" }
            require(first().isLetter()) { "Ugyldig første tegn ${first()} i $this, må være stor bokstav" }
            requireDigits(substring(3), 4)
        }
    }

    companion object {
        private const val TIDENT_LENGTH = 7
    }

    override fun toString() = verdi
}