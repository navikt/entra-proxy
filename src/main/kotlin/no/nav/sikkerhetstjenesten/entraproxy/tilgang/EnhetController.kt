package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppeTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.felles.security.OAuth2RequireOBO
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "EnhetController", description = "Denne kontrolleren skal brukes i produksjon")
@RestController
@RequestMapping(API_V1)
class EnhetController(
    private val entra: EntraTjeneste,
    private val gruppe: EntraGruppeTjeneste,
    private val oid: EntraOidTjeneste,
    private val norg: NorgTjeneste) {

    @GetMapping("navn/{enhetsnummer}", produces = [TEXT_PLAIN_VALUE])
    @Operation(summary = "Hent enhetsnavn fra NORG")
    fun enhetsnavn(@PathVariable enhetsnummer: Enhetnummer) =
        norg.navn(enhetsnummer)

    @GetMapping("enhet/ansatt/{navIdent}")
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt fra Entra")
    fun enheterForAnsatt(@PathVariable navIdent: AnsattId) =
        entra.enheter(navIdent)

    @GetMapping("enhet")
    @OAuth2RequireOBO
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt fra Entra, forutsetter OBO-flow")
    fun enheterForAnsatt(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal) =
        entra.enheter(principal.requiredAttribute(NAVIDENT, ::AnsattId))

    @GetMapping("enhet/{enhetsnummer}")
    @Operation(summary = "Hent alle medlemmer fra Entra for en gitt enhet")
    fun medlemmerForEnhet(@PathVariable enhetsnummer: Enhetnummer) =
        medlemmerIGruppe(oid, gruppe, enhetsnummer.gruppeNavn)
}
