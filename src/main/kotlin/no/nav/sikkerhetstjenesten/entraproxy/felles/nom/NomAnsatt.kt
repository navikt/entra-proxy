package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId

data class NomAnsatt(
    val navident: AnsattId,
    val visningsnavn: String,
    val gjeldendeSektor: String,
    val orgTilknytninger: Set<NomOrgTilknytning> = emptySet()
)
