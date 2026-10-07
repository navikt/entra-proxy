package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.felles.rest.DevController
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@DevController(
    value = ["/${DEV}/nom"],
    name = "NomDevController")
class NomController(
    private val nom: NomTjeneste) {

    @GetMapping("/org/{ansattId}")
    fun orgTilknytninger(@PathVariable ansattId: AnsattId) =
        nom.orgTilknytninger(ansattId)

}