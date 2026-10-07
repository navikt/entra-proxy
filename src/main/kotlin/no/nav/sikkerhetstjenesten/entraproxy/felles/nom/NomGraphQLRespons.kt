package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId

data class NomGraphQLRespons(val navident: AnsattId, val visningsnavn: String, val orgTilknytninger: Set<NomOrgTilknytning> = emptySet()) {

}

