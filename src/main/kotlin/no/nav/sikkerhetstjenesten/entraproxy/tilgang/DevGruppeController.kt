package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppeTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/${DEV}")
@ConditionalOnNotProd
@Tag(name = "DevGruppeController",
    description = "Denne kontrolleren er bare tilgjengelig i dev og skal kun brukes til interaktiv testing")
class DevGruppeController(
    private val gruppeTjeneste: EntraGruppeTjeneste,
    private val oidTjeneste: EntraOidTjeneste,
    private val norgTjeneste: NorgTjeneste) {

    @GetMapping("enhet/{enhetsnummer}")
    fun enhetMedlemmer(@PathVariable enhetsnummer: Enhetnummer) =
        medlemmer(enhetsnummer.gruppeNavn)

    @GetMapping("navn/{enhetsnummer}")
    fun norgNavn(@PathVariable enhetsnummer: Enhetnummer) =
        norgTjeneste.navn(enhetsnummer)

    @GetMapping("tema/{tema}")
    fun temaMedlemmer(@PathVariable tema: Tema) =
        medlemmer(tema.gruppeNavn)

    @GetMapping("gruppe/medlemmer")
    fun medlemmer(@RequestParam gruppeNavn: String) =
        oidTjeneste.gruppeOid(gruppeNavn)?.let {
            gruppeTjeneste.medlemmerIGruppe(it)
        }

    @GetMapping("gruppe/medlemmer/sok")
    fun medlemmerIGrupperSomMatcher(@RequestParam gruppeNavn: String) =
        gruppeTjeneste.medlemmerIGrupperSomMatcher(gruppeNavn)

    @GetMapping("gruppe/medlemmer/sok/per-ansatt")
    fun grupperPerAnsattSomMatcher(@RequestParam gruppeNavn: String) =
        gruppeTjeneste.grupperPerAnsattSomMatcher(gruppeNavn)
}
