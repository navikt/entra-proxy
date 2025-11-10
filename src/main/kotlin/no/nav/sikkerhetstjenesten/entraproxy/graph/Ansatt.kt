package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits


data class Ansatt(@JsonValue val id: String, val displayName: String = "Intet navn"): Comparable<Ansatt> {

    init {
        with(id) {
            require(length == ANSATTID_LENGTH) { "Ugyldig lengde $length for $this, forventet ${ANSATTID_LENGTH}" }
            require(first().isLetter()) { "Ugyldig første tegn ${first()} i $this, må være stor bokstav" }
            requireDigits(
                substring(1),
                6
            )
        }
    }

    override fun compareTo(other: Ansatt): Int = id.compareTo(other.id)

    companion object {
        const val ANSATTID_LENGTH = 7
    }

    override fun toString() = id
}
