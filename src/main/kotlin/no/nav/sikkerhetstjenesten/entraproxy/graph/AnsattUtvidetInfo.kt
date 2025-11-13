package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonProperty

data class AnsattUtvidetInfo(val id: String,
                             @param:JsonProperty("jobTitle")  val tIdent: String = UKJENT,
                             @param:JsonProperty("mail")  val epost: String = UKJENT,
                             @param:JsonProperty("officeLocation")  val enhet: String = UKJENT,
                             @param:JsonProperty("onPremisesSamAccountName")  val navIdent: String = UKJENT,
                             @param:JsonProperty("displayName")  val navn: String = UKJENT,
                             @param:JsonProperty("givenName")  val fornavn: String = UKJENT,
                             @param:JsonProperty("surname")  val etternavn: String = UKJENT,
): Comparable<AnsattUtvidetInfo> {

    override fun compareTo(other: AnsattUtvidetInfo): Int = etternavn.compareTo(other.etternavn)

    companion object {
        private const val UKJENT = "N/A"
    }
}
