package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.requireDigits

data class TIdent(@JsonValue val verdi: String) {
    init {
        with(verdi) {
            require(length == TIDENT_LENGTH) { "Ugyldig lengde $length for $this, forventet $TIDENT_LENGTH" }
            require(first().isLetter()) { "Ugyldig første tegn ${first()} i $this, må være stor bokstav" }
            requireDigits(substring(TIDENT_PREFIX_LENGTH), TIDENT_LENGTH - TIDENT_PREFIX_LENGTH)
        }
    }

    companion object {

        private const val TIDENT_PREFIX_LENGTH = 3
        private const val TIDENT_LENGTH = 7
        fun parse(verdi: String)  =
            runCatching {
                TIdent(verdi)
        }.getOrNull()
    }

    override fun toString() = verdi
}
