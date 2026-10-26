package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent

data class NomAnsatt(
    val navident: AnsattId,
    val visningsnavn: String,
    val gjeldendeSektor: String,
    val orgTilknytninger: Set<NomOrgTilknytning> = emptySet())

data class NomEnhetAnsatte(
    val id: NomIdent,
    val navn: String?,
    val ansatte: Set<NomAnsatt> = emptySet())
