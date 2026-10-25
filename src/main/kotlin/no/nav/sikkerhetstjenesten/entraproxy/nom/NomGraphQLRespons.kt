package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent

data class NomGraphQLRespons(
    val navident: AnsattId,
    val visningsnavn: String,
    val gjeldendeSektor: Sektor,
    val orgTilknytninger: Set<NomOrgTilknytning> = emptySet(),
) {
    enum class Sektor(val tekst: String) {
        NAV_STATLIG("Statlig"),
        NAV_KOMMUNAL("Kommunal"),
        EKSTERN("Ekstern")
    }
}

data class NomBulkGraphQLRespons(val ressurs: NomGraphQLRespons? = null, val erDagligOppfolging: Boolean = false)

data class NomOrgEnhetRespons(val orgEnhet: NomEnhetMedAnsatte? = null)
data class NomEnhetMedAnsatte(
    val id: NomIdent,
    val navn: String,
    val orgTilknytninger: List<NomBulkGraphQLRespons> = emptyList(),
)
