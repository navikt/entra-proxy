package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.NAVIDENT

data class AnsattUtvidetInfo(val id: String,
                             @param:JsonAlias("jobTitle")  val tIdent: String,
                             @param:JsonAlias("mail")  val epost: String,
                             @param:JsonAlias("officeLocation")  val enhet: String,
                             @param:JsonAlias(NAVIDENT)  val navIdent: String,
                             @param:JsonAlias("displayName")  val navn: String,
                             @param:JsonAlias("givenName")  val fornavn: String,
                             @param:JsonAlias("surname")
                             val etternavn: String): Comparable<AnsattUtvidetInfo> {

    override fun compareTo(other: AnsattUtvidetInfo): Int = etternavn.compareTo(other.etternavn)
}
