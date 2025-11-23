package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.NAVIDENT
import java.util.UUID


open class Ansatt(val id: UUID,
                  val navIdent: AnsattId,
                  val navn: String,
                  val fornavn: String,
                  val etternavn: String): Comparable<Ansatt> {

    override fun compareTo(other: Ansatt): Int = etternavn.compareTo(other.etternavn)
}

class UtvidetAnsatt(id: UUID,
                    @JsonAlias(NAVIDENT)  navIdent: AnsattId,
                    @JsonAlias("displayName")  navn: String,
                    @JsonAlias("givenName")  fornavn: String,
                    @JsonAlias("surname")  etternavn: String,
                    @param:JsonAlias("jobTitle")  val tIdent: String,
                    @param:JsonAlias("mail")  val epost: String,
                    @param:JsonAlias("officeLocation")  val enhet: String) : Ansatt(id,navIdent,navn,fornavn,etternavn)