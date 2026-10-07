package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.springframework.stereotype.Service

@Service
class NomTjeneste(private val graph: NomSyncGraphQLClientAdapter) {


    fun ansattData(ansattId: AnsattId) =
        graph.orgTilknytninger(ansattId.verdi)

}