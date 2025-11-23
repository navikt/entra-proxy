package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.Requirements.requireDigits
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.NAVIDENT
import java.util.UUID


open class Ansatt(val id: UUID,
                  @param:JsonAlias(NAVIDENT) val navIdent: AnsattId,
                  @param:JsonAlias("displayName") val navn: String,
                   @param:JsonAlias("givenName") val fornavn: String,
                   @param:JsonAlias("surname") val etternavn: String): Comparable<Ansatt> {

    override fun compareTo(other: Ansatt): Int = etternavn.compareTo(other.etternavn)
}

class AnsattUtvidetInfo( id: UUID,
                         @JsonAlias(NAVIDENT)  navIdent: AnsattId,
                         @JsonAlias("displayName")  navn: String,
                        @JsonAlias("givenName")  fornavn: String,
                        @JsonAlias("surname")  etternavn: String,
                        @param:JsonAlias("jobTitle")  val tIdent: String,
                        @param:JsonAlias("mail")  val epost: String,
                        @param:JsonAlias("officeLocation")  val enhet: String): Ansatt(id,navIdent,navn,fornavn,etternavn)