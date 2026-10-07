package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.felles.Enhetnummer

data class NomRespons(val navident: AnsattId, val orgTilknytninger: Set<OrgTilknytning> = emptySet()) {

    data class OrgTilknytning(val orgEnhet: OrgEnhet)  {
        data class OrgEnhet(
            val id: String,
            val navn: String,
            val tilgangsenhetId: Enhetnummer,
        )
    }
}
