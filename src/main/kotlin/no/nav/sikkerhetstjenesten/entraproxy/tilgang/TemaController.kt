package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.API_V1
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppeTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.felles.security.OAuth2RequireCCF
import no.nav.sikkerhetstjenesten.felles.security.OAuth2RequireOBO
import no.nav.sikkerhetstjenesten.felles.security.SecurityExtensions.requiredAttribute
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "TemaController", description = "Disse endepunktene skal brukes i produksjon")
@RestController
@RequestMapping(API_V1)
class TemaController(
    private val entra: EntraTjeneste,
    private val gruppe: EntraGruppeTjeneste,
    private val oid: EntraOidTjeneste) {

    @GetMapping("tema/ansatt/{navIdent}")
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt fra Entra, forutsetter CC-flow")
    @OAuth2RequireCCF
    fun temaForAnsatt(@PathVariable navIdent: AnsattId) =
        entra.tema(navIdent)

    @GetMapping("tema")
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt fra Entra, forutsetter OBO-flow")
    @OAuth2RequireOBO
    fun temaForAnsatt(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal) =
        entra.tema(principal.requiredAttribute(NAVIDENT, ::AnsattId))

    @GetMapping("tema/{tema}")
    @Operation(summary = "Hent alle medlemmer fra Entra for et gitt tema")
    fun medlemmerForTema(@PathVariable tema: Tema) =
        oid.gruppeOid(tema.gruppeNavn)?.let {
            gruppe.medlemmerIGruppe(it)
        } ?: emptySet()
}
