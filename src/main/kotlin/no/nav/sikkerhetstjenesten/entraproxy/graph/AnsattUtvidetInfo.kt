package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class AnsattUtvidetInfo(val id: String,
                             @param:JsonProperty("jobTitle") @param:JsonAlias("tIdent") val tIdent: String = UKJENT,
                             @param:JsonProperty("mail") @param:JsonAlias("epost") val epost: String = UKJENT,
                             @param:JsonProperty("officeLocation") @param:JsonAlias("enhet") val enhet: String = UKJENT,
                             @param:JsonProperty("onPremisesSamAccountName") @param:JsonAlias("navIdent") val navIdent: String = UKJENT,
                             @param:JsonProperty("displayName") @param:JsonAlias("visningNavn") val navn: String = UKJENT,
                             @param:JsonProperty("givenName") @param:JsonAlias("firstName") val fornavn: String = UKJENT,
                             @param:JsonProperty("surname") @param:JsonAlias("lastName") val etternavn: String = UKJENT,
): Comparable<AnsattUtvidetInfo> {

    override fun compareTo(other: AnsattUtvidetInfo): Int = etternavn.compareTo(other.etternavn)

    companion object {
        private const val UKJENT = "N/A"
    }
}
