package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.felles.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.felles.nom.NomTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController.Companion.API_V1
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.OID
import no.nav.sikkerhetstjenesten.felles.security.OAuth2RequireCCF
import no.nav.sikkerhetstjenesten.felles.security.OAuth2RequireOBO
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

//@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
//@SecurityRequirement(name = "bearerAuth")
@Tag(name = "EntraController", description = "Denne kontrolleren skal brukes i produksjon")
@RestController
@RequestMapping(API_V1)
class EntraController(private val entra: EntraTjeneste, private val oid: EntraOidTjeneste, private val nom: NomTjeneste) {
    @GetMapping("enhet/ansatt/{navIdent}")
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt")
    fun enheterForAnsatt(@PathVariable navIdent: AnsattId) =
        oid.ansattOid(navIdent)?.let {
            entra.enheter(navIdent, it)
        } ?: emptySet()

    @GetMapping("enhet")
    @OAuth2RequireOBO
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt, forutsetter OBO-flow")
    fun enheterForAnsatt(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal) =
            entra.enheter(principal.requiredAttribute(NAVIDENT, ::AnsattId),principal.requiredAttribute(OID,UUID::fromString))

    @GetMapping("tema/ansatt/{navIdent}")
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt, forutsetter CC-flow")
    @OAuth2RequireCCF
    fun temaForAnsatt(@PathVariable navIdent: AnsattId) =
            oid.ansattOid(navIdent)?.let {
                entra.tema(navIdent, it)
            } ?: emptySet()

    @GetMapping("tema")
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt, forutsetter OBO-flow")
    @OAuth2RequireOBO
    fun temaForAnsatt(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal) =
            entra.tema(principal.requiredAttribute(NAVIDENT, ::AnsattId), principal.requiredAttribute(OID,UUID::fromString))

    @GetMapping("enhet/{enhetsnummer}")
    @Operation(summary = "Hent alle medlemmer for en gitt enhet")
    fun medlemmerForEnhet(@PathVariable enhetsnummer: Enhetnummer) =
        medlemmerIGruppe(enhetsnummer.gruppeNavn)

    @GetMapping("tema/{tema}")
    @Operation(summary = "Hent alle medlemmer for et gitt tema")
    fun medlemmerForTema(@PathVariable tema: Tema) =
        medlemmerIGruppe(tema.gruppeNavn)

    @GetMapping("ansatt/{navIdent}")
    @Operation(summary = "Hent informasjon om ansatt ved bruk av NavIdent")
    fun utvidetAnsattForNavIdent(@PathVariable navIdent: AnsattId) =
        entra.utvidetAnsatt(navIdent)

    @GetMapping("ansatt/tident/{tIdent}")
    @Operation(summary = "Hent informasjon om ansatt ved bruk av (AAA1234)")
    fun utvidetAnsattForTIdent(@PathVariable tIdent: TIdent) =
        entra.utvidetAnsatt(tIdent)

    @GetMapping("/ansatt/tilganger/{navIdent}")
    @Operation(summary = "Hent informasjon om ansatts tilganger")
    fun grupperForAnsatt(@PathVariable navIdent: AnsattId) =
        oid.ansattOid(navIdent)?.let {
            entra.grupperForAnsatt(navIdent, it)
        } ?: emptySet()

    @GetMapping("gruppe/medlemmer")
    @Operation(summary = "Hent ansatte i en gitt gruppe")
    fun gruppeMedlemmer(@RequestParam gruppeNavn: String) =
        medlemmerIGruppe(gruppeNavn)

    private fun medlemmerIGruppe(gruppeNavn: String) =
        oid.gruppeOid(gruppeNavn)?.let {
            entra.medlemmerIGruppe( gruppeNavn, it)
        } ?: emptySet()

    @GetMapping("gruppe/antall")
    @Operation(summary = "Hent antall medlemmer i en gitt gruppe")
    fun antallMedlemmerIGruppe(@RequestParam gruppeNavn: String) =
        oid.gruppeOid(gruppeNavn)?.let {
            entra.antallMedlemmerIGruppe(it)
        } ?: "0"

    @GetMapping("nom/enhet/{navIdent}")
    @Operation(summary = "Hent org-tilknytninger for ansatt fra NOM")
    fun orgTilknytningerForAnsatt(@PathVariable navIdent: AnsattId) =
        nom.orgTilknytninger(navIdent)

    companion object {
        const val API_V1 = "/api/v1"
    }

}

fun <R> OAuth2AuthenticatedPrincipal.requiredAttribute(attributeName: String, mapper: (String) -> R): R =
    mapper(requireNotNull(getAttribute(attributeName)) { "Mangler $attributeName i OBO-token" })