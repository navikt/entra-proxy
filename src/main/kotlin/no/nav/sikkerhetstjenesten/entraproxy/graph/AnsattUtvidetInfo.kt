package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.NAVIDENT

data class AnsattUtvidetInfo(val id: String,
                             @param:JsonAlias("jobTitle")  val tIdent: String = UKJENT,
                             @param:JsonAlias("mail")  val epost: String = UKJENT,
                             @param:JsonAlias("officeLocation")  val enhet: String = UKJENT,
                             @param:JsonAlias(NAVIDENT)  val navIdent: String = UKJENT,
                             @param:JsonAlias("displayName")  val navn: String = UKJENT,
                             @param:JsonAlias("givenName")  val fornavn: String = UKJENT,
                             @param:JsonAlias("surname")
                             val etternavn: String = UKJENT,
): Comparable<AnsattUtvidetInfo> {

    override fun compareTo(other: AnsattUtvidetInfo): Int = etternavn.compareTo(other.etternavn)

    companion object {
        private const val UKJENT = "N/A"
    }
}
