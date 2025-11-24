package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.security.token.support.spring.UnprotectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.DEV
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@UnprotectedRestController(value = ["/${DEV}"])
@ConditionalOnNotProd
@Tag(name = "DevEntraController", description = "Denne kontrolleren er bare tilgjengelig i dev og skal kun brukes til testing")
class DevEntraController (private val entraTjeneste: EntraTjeneste, private val oidTjeneste: EntraOidTjeneste, private val norgTjeneste: NorgTjeneste) {

    @GetMapping("enhet/ansatt/{navIdent}")
    fun enheter(@PathVariable navIdent: AnsattId) =
        oidTjeneste.ansattOid(navIdent)?.let {
            entraTjeneste.enheter(navIdent, it)
        }

    @GetMapping("tema/ansatt/{navIdent}")
    fun temaer(@PathVariable navIdent: AnsattId) =
        oidTjeneste.ansattOid(navIdent)?.let {
            entraTjeneste.tema(navIdent, it)
        }

    @GetMapping("enhet/{enhetsnummer}")
    fun enhetMedlemmer(@PathVariable enhetsnummer: Enhetnummer) =
        medlemmer(enhetsnummer.gruppeNavn)

    @GetMapping("navn/{enhetsnummer}")
    fun norgNavn(@PathVariable enhetsnummer: Enhetnummer) =
       norgTjeneste.navnFor(enhetsnummer)

    @GetMapping("tema/{tema}")
    fun temaMedlemmer(@PathVariable tema: Tema) =
        medlemmer(tema.gruppeNavn)

    @GetMapping("gruppe/medlemmer")
     fun medlemmer(gruppeNavn: String) =
        oidTjeneste.gruppeOid(gruppeNavn)?.let {
            entraTjeneste.medlemmer( it)
        }

    @GetMapping("ansatt/{navIdent}")
    fun utvidetAnsatt(@PathVariable navIdent: AnsattId) =
        entraTjeneste.utvidetAnsatt(navIdent)

}