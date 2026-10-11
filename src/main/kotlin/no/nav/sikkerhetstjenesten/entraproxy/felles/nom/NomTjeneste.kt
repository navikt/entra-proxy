package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import org.springframework.stereotype.Service

@Service
class NomTjeneste(private val nom: NomPort) {


    fun orgData(ident: AnsattId): NomAnsatt =
        nom.ansatt(ident)

    fun orgDataBulk(identer: Set<AnsattId>): Set<NomAnsatt> =
        nom.ansatte(identer)

    fun ansatteForTilgangsenhet(tilgangsenhetId: Enhetnummer): Set<NomAnsatt> =
        nom.ansatteForTilgangsenhet(tilgangsenhetId)

}