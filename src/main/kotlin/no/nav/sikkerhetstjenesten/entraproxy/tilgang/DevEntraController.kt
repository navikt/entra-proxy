package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.security.token.support.spring.UnprotectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.DEV
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@UnprotectedRestController(value = ["/${DEV}"])
@ConditionalOnNotProd
@Tag(name = "DevEntraController", description = "Denne kontrolleren skal kun brukes til testing")
class DevEntraController (private val entraTjeneste: EntraTjeneste, private val oidTjeneste: AnsattOidTjeneste) {

    @GetMapping("ansatt/enheter/{ansattId}")
    fun enheter(@PathVariable ansattId: AnsattId) =
        oidTjeneste.oid(ansattId).let {
            entraTjeneste.enheter(ansattId, it)
        }

    @GetMapping("ansatt/temaer/{ansattId}")
    fun temaer(@PathVariable ansattId: AnsattId) =
        oidTjeneste.oid(ansattId).let {
            entraTjeneste.tema(ansattId, it)
        }

    @GetMapping("enheter/medlemmer/{enhetsnummer}")
    fun enhetMedlemmer(@PathVariable enhetsnummer: Enhetnummer) =
        entraTjeneste.gruppeIdForEnhet(enhetsnummer)?.let {
            entraTjeneste.medlemmer( it)
        }
    @GetMapping("tema/medlemmer/{tema}")
    fun temaMedlemmer(@PathVariable tema: Tema) =
        entraTjeneste.gruppeIdForTema(tema)?.let {
            entraTjeneste.medlemmer( it)
        }
}