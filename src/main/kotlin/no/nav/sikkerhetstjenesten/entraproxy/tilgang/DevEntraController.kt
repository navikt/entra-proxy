package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.security.token.support.spring.UnprotectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.OidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.DEV
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@UnprotectedRestController(value = ["/${DEV}"])
@ConditionalOnNotProd
@Tag(name = "DevEntraController", description = "Denne kontrolleren skal kun brukes til testing")
class DevEntraController (private val entraTjeneste: EntraTjeneste, private val oidTjeneste: OidTjeneste) {

    @GetMapping("enhet/ansatt/{navIdent}")
    fun enheter(@PathVariable @Schema(implementation = AnsattId::class) navIdent: AnsattId) =
        oidTjeneste.oid(navIdent)?.let {
            entraTjeneste.enheter(navIdent, it)
        }

    @GetMapping("tema/ansatt/{navIdent}")
    fun temaer(@PathVariable @Schema(implementation = AnsattId::class) navIdent: AnsattId) =
        oidTjeneste.oid(navIdent)?.let {
            entraTjeneste.tema(navIdent, it)
        }

    @GetMapping("enhet/{enhetsnummer}")
    fun enhetMedlemmer(@PathVariable @Schema(implementation = Enhetnummer ::class) enhetsnummer: Enhetnummer) =
        medlemmer(enhetsnummer.gruppeNavn)

    @GetMapping("tema/{tema}")
    fun temaMedlemmer(@PathVariable @Schema(implementation = Tema::class) tema: Tema) =
        medlemmer(tema.gruppeNavn)

    private fun medlemmer(gruppeNavn: String) =
        oidTjeneste.gruppeId(gruppeNavn)?.let {
            entraTjeneste.medlemmer( it)
        }
}