package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.Requirements.requireDigits


data class Ansatt(val id: String,
                  @param:JsonProperty("visningNavn") @param:JsonAlias("displayName") val navn: String =UKJENT,
                  @param:JsonProperty("fornavn") @param:JsonAlias("firstName") val fornavn: String = UKJENT,
                  @param:JsonProperty("etternavn") @param:JsonAlias("lastName") val etternavn: String = UKJENT,
                  ): Comparable<Ansatt> {

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

    override fun compareTo(other: Ansatt): Int = etternavn.compareTo(other.etternavn)

    companion object {
        private const val UKJENT = "N/A"
        const val ANSATTID_LENGTH = 7
    }
}
