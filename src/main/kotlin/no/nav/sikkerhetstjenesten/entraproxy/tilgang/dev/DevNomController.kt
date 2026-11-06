package no.nav.sikkerhetstjenesten.entraproxy.tilgang.dev

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomTjeneste
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/${DEV}")
@ConditionalOnNotProd
@Tag(name = "DevNomController",
    description = "Disse endepunktene er bare tilgjengelig i dev og skal kun brukes til interaktiv testing")
class DevNomController(private val nom: NomTjeneste) {

    @GetMapping("/nom/{ansattId}")
    fun orgData(@PathVariable ansattId: AnsattId) =
        nom.ansatt(ansattId)

    @PostMapping("/nom/bulk")
    fun orgDataBulk(@RequestBody identer: Set<AnsattId>) =
        nom.ansatte(identer)

    @GetMapping("/nom/enhet/{nomId}")
    fun ansatteForNomIdent(@PathVariable nomId: NomIdent) =
        nom.ansatteForNomIdent(nomId)
}
