package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent

interface NomPort {
    fun ansatt(ident: AnsattId): NomAnsatt
    fun ansatte(identer: Set<AnsattId>): Set<NomAnsatt>
    fun ansatteForEnhet(enhet: Enhetnummer): Set<NomAnsatt>
    fun ansatteForNomIdent(ident: NomIdent): Set<NomAnsatt>
}
