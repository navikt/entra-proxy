package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

interface NomPort {
    fun ansatt(ident: AnsattId): NomAnsatt
    fun ansatte(identer: Set<AnsattId>): Set<NomAnsatt>
    fun ansatteForTilgangsenhet(tilgangsenhetId: Enhetnummer): Set<NomAnsatt>
}
