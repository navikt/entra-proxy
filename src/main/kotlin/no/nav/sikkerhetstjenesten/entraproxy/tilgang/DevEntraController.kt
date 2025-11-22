package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
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
class DevEntraController (private val entra: EntraTjeneste, private val oid: EntraOidTjeneste, private val norg: NorgTjeneste) {

    @GetMapping("enhet/ansatt/{navIdent}")
    fun enheter(@PathVariable navIdent: AnsattId) =
        oid.oid(navIdent)?.let {
            entra.enheter(navIdent, it)
        }

    @GetMapping("tema/ansatt/{navIdent}")
    fun temaer(@PathVariable navIdent: AnsattId) =
        oid.oid(navIdent)?.let {
            entra.tema(navIdent, it)
        }

    @GetMapping("enhet/{enhetsnummer}")
    fun enhetMedlemmer(@PathVariable enhetsnummer: Enhetnummer) =
        medlemmer(enhetsnummer.gruppeNavn)

    @GetMapping("navn/{enhetsnummer}")
    fun norgNavn(@PathVariable enhetsnummer: Enhetnummer) =
       norg.navnFor(enhetsnummer)

    @GetMapping("tema/{tema}")
    fun temaMedlemmer(@PathVariable tema: Tema) =
        medlemmer(tema.gruppeNavn)

    @GetMapping("gruppe/medlemmer")
     fun medlemmer(gruppeNavn: String) =
        oid.gruppeId(gruppeNavn)?.let {
            entra.medlemmer( it)
        }

    @Operation(
        summary= "Hente utansattinfo fra Entra ",
        description = """
            Hente spesifikke auditlogg-informasjon knyttet til ansattinfo fra Entra basert på navIdent
        """""")
    @GetMapping("ansattUtvidet/{navIdent}")
    fun ansattUtvidet(@PathVariable navIdent: String) =
        entra.ansattUtvidet(navIdent)

}