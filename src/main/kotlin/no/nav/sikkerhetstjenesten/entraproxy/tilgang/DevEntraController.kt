package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/${DEV}")
@ConditionalOnNotProd
@Tag(name = "DevEntraController", description = "Denne kontrolleren er bare tilgjengelig i dev og skal kun brukes til interaktiv testing")
class DevEntraController (private val entraTjeneste: EntraTjeneste, private val oidTjeneste: EntraOidTjeneste, private val norgTjeneste: NorgTjeneste, private val nom: NomTjeneste) {

    @GetMapping("/nom/{ansattId}")
    fun orgData(@PathVariable ansattId: AnsattId) =
        nom.ansatt(ansattId)

    @PostMapping("/nom/bulk")
    fun orgDataBulk(@RequestBody identer: Set<AnsattId>) =
        nom.ansatte(identer)

    @GetMapping("/nom/enhet/{nomId}")
    fun ansatteForNomIdent(@PathVariable nomId: NomIdent) =
        nom.ansatteForNomIdent(nomId)

    @GetMapping("enhet/ansatt/{navIdent}")
    fun enheter(@PathVariable navIdent: AnsattId) =
        entraTjeneste.enheter(navIdent)

    @GetMapping("tema/ansatt/{navIdent}")
    fun temaer(@PathVariable navIdent: AnsattId) =
        entraTjeneste.tema(navIdent)

    @GetMapping("tilganger/ansatt/{navIdent}")
    fun ansatteGrupper(@PathVariable navIdent: AnsattId) =
        entraTjeneste.grupper(navIdent)

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
            entraTjeneste.medlemmerIGruppe(it)
        }

    @GetMapping("gruppe/medlemmer/sok")
    fun medlemmerIGrupperSomMatcher(@RequestParam gruppeNavn: String) =
        entraTjeneste.medlemmerIGrupperSomMatcher(gruppeNavn)

    @GetMapping("gruppe/medlemmer/sok/per-ansatt")
    fun grupperPerAnsattSomMatcher(@RequestParam gruppeNavn: String) =
        entraTjeneste.grupperPerAnsattSomMatcher(gruppeNavn)

    @GetMapping("ansatt/{navIdent}")
    fun utvidetAnsatt(@PathVariable navIdent: AnsattId) =
        entraTjeneste.utvidetAnsatt(navIdent)

    @GetMapping("ansatt/tident/{tIdent}")
    fun utvidetAnsatt(@PathVariable tIdent: TIdent) =
        entraTjeneste.utvidetAnsatt(tIdent)

}

