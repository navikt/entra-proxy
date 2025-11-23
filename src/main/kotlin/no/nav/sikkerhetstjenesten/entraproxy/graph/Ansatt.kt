package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.Requirements.requireDigits


data class Ansatt(val id: AnsattId,
                  @param:JsonAlias("displayName") val navn: String =UKJENT,
                   @param:JsonAlias("firstName") val fornavn: String = UKJENT,
                   @param:JsonAlias("lastName") val etternavn: String = UKJENT,
                  ): Comparable<Ansatt> {

    override fun compareTo(other: Ansatt): Int = etternavn.compareTo(other.etternavn)

    companion object {
        private const val UKJENT = "N/A"
        const val ANSATTID_LENGTH = 7
    }
}
