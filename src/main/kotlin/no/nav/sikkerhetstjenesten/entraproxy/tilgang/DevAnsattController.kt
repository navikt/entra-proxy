package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/${DEV}")
@ConditionalOnNotProd
@Tag(name = "DevAnsattController",
    description = "Denne kontrolleren er bare tilgjengelig i dev og skal kun brukes til interaktiv testing")
class DevAnsattController(private val entraTjeneste: EntraTjeneste) {

    @GetMapping("enhet/ansatt/{navIdent}")
    fun enheter(@PathVariable navIdent: AnsattId) =
        entraTjeneste.enheter(navIdent)

    @GetMapping("tema/ansatt/{navIdent}")
    fun temaer(@PathVariable navIdent: AnsattId) =
        entraTjeneste.tema(navIdent)

    @GetMapping("tilganger/ansatt/{navIdent}")
    fun ansatteGrupper(@PathVariable navIdent: AnsattId) =
        entraTjeneste.grupper(navIdent)

    @GetMapping("ansatt/{navIdent}")
    fun utvidetAnsatt(@PathVariable navIdent: AnsattId) =
        entraTjeneste.utvidetAnsatt(navIdent)

    @GetMapping("ansatt/tident/{tIdent}")
    fun utvidetAnsatt(@PathVariable tIdent: TIdent) =
        entraTjeneste.utvidetAnsatt(tIdent)
}
