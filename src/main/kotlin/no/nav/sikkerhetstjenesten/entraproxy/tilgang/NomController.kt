package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.API_V1
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "NomController", description = "Denne kontrolleren skal brukes i produksjon")
@RestController
@RequestMapping(API_V1)
class NomController(private val nom: NomTjeneste) {

    @GetMapping("nom/enhet/{navIdent}")
    @Operation(summary = "Hent ansatt og org-tilknytninger fra NOM")
    fun ansatt(@PathVariable navIdent: AnsattId) =
        nom.ansatt(navIdent)

    @PostMapping("nom/enhet/bulk")
    @Operation(summary = "Hent ansatte og org-tilknytninger fra NOM")
    fun ansatte(@RequestBody identer: Set<AnsattId>) =
        nom.ansatte(identer)

    @GetMapping("nom/ansatte/{tilgangsenhetId}")
    @Operation(summary = "Hent ansatte i en tilgangsenhet fra NOM")
    fun ansatteForEnhet(@PathVariable tilgangsenhetId: Enhetnummer) =
        nom.ansatteForEnhet(tilgangsenhetId)

    @GetMapping("nom/ansatte/enhet/{nomId}")
    @Operation(summary = "Hent ansatte i en nom-enhet og underliggende enheter")
    fun ansatteForNomIdent(@PathVariable nomId: NomIdent) =
        nom.ansatteForNomIdent(nomId)
}
