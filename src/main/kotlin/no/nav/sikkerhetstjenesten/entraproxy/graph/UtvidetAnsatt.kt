package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

 class UtvidetAnsatt(id: String,
                     navn:String,
                     fornavn: String,
                     etternavn: String,
                     @param:JsonProperty("jobTitle") @param:JsonAlias("tIdent") val tIdent: String = UKJENT,
                     @param:JsonProperty("mail") @param:JsonAlias("epost") val epost: String = UKJENT,
                     @param:JsonProperty("officeLocation") @param:JsonAlias("enhet")val enhet: String = UKJENT,
                     @param:JsonProperty("onPremisesSamAccountName") @param:JsonAlias("navIdent") val navIdent: String = UKJENT,
): Ansatt(id,navn,fornavn,etternavn)
