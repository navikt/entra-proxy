package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent

data class NomGraphQLRespons(val navident: AnsattId, val visningsnavn: String, val gjeldendeSektor: String,val orgTilknytninger: Set<NomOrgTilknytning> = emptySet())
data class NomBulkGraphQLRespons(val ressurs: NomGraphQLRespons? = null)

data class NomOrgEnhetRespons(val orgEnhet: NomEnhetMedAnsatte? = null)
data class NomEnhetMedAnsatte(val id: NomIdent, val navn: String, val orgTilknytninger: List<NomBulkGraphQLRespons> = emptyList())
