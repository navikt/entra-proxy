package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import org.springframework.stereotype.Service

@Service
class NomTjeneste(private val graph: NomSyncGraphQLClientAdapter) {


    fun orgData(ident: AnsattId) =
        graph.orgData(ident.verdi)

    fun orgDataBulk(identer: Set<AnsattId>) =
        graph.orgDataBulk(identer.map { it.verdi }.toSet())

    fun ansatteForTilgangsenhet(tilgangsenhetId: Enhetnummer) =
        graph.ansatteForTilgangsenhet(tilgangsenhetId.verdi)

}