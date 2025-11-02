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

    @GetMapping("ansatt/enhet/{ansattId}")
    fun enheter(@PathVariable ansattId: AnsattId) =
        oidTjeneste.oid(ansattId)?.let {
            entraTjeneste.enheter(ansattId, it)
        }

    @GetMapping("ansatt/tema/{ansattId}")
    fun temaer(@PathVariable ansattId: AnsattId) =
        oidTjeneste.oid(ansattId)?.let {
            entraTjeneste.tema(ansattId, it)
        }

    @GetMapping("enhet/medlemmer/{enhetsnummer}")
    fun enhetMedlemmer(@PathVariable enhetsnummer: Enhetnummer) =
        medlemmer(enhetsnummer.gruppeNavn)

    @GetMapping("tema/medlemmer/{tema}")
    fun temaMedlemmer(@PathVariable tema: Tema) =
        medlemmer(tema.gruppeNavn)

    private fun medlemmer(gruppeNavn: String) =  entraTjeneste.gruppeId(gruppeNavn)?.let {
        entraTjeneste.medlemmer( it)
    }
}