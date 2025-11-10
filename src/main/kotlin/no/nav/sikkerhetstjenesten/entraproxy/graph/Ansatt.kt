package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue


data class Ansatt(@JsonValue val id: String, @JsonValue val displayName: String): Comparable<Ansatt> {

    init {
        with(id) {
            require(length == ANSATTID_LENGTH) { "Ugyldig lengde $length for $this, forventet ${ANSATTID_LENGTH}" }
            require(first().isLetter()) { "Ugyldig første tegn ${first()} i $this, må være stor bokstav" }
            no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits(
                substring(1),
                6
            )
        }
    }

    override fun compareTo(other: Ansatt): Int = displayName.compareTo(other.displayName)

    companion object {
        const val ANSATTID_LENGTH = 7
    }

    override fun toString() = id
}
