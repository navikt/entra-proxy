package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AuthContext.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AuthContext.Companion.OID
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.security.Authorities.OAuth2RequireCCF
import no.nav.sikkerhetstjenesten.entraproxy.security.Authorities.OAuth2RequireOBO
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController.Companion.API_V1
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "EntraController", description = "Denne kontrolleren skal brukes i produksjon")
@RestController
@RequestMapping(API_V1)
class EntraController(private val entraTjeneste: EntraTjeneste, private val oidTjeneste: EntraOidTjeneste) {
    @GetMapping("enhet/ansatt/{navIdent}")
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt, forutsetter CC-flow")
    @OAuth2RequireCCF
    fun enheterForAnsatt(@PathVariable navIdent: AnsattId) =
        oidTjeneste.ansattOid(navIdent)?.let { entraTjeneste.enheter(navIdent, it) } ?: emptySet()

    @GetMapping("enhet")
    @OAuth2RequireOBO
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt, forutsetter OBO-flow")
    fun enheterForAnsatt(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal) =
            entraTjeneste.enheter(principal.requiredAttribute(NAVIDENT, ::AnsattId),principal.requiredAttribute(OID,UUID::fromString))

    @GetMapping("tema/ansatt/{navIdent}")
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt, forutsetter CC-flow")
    @OAuth2RequireCCF
    fun temaForAnsatt(@PathVariable navIdent: AnsattId) =
            oidTjeneste.ansattOid(navIdent)?.let { entraTjeneste.tema(navIdent, it) } ?: emptySet()

    @GetMapping("tema")
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt, forutsetter OBO-flow")
    @OAuth2RequireOBO
    fun temaForAnsatt(@AuthenticationPrincipal principal: OAuth2AuthenticatedPrincipal) =
            entraTjeneste.tema(principal.requiredAttribute(NAVIDENT, ::AnsattId), principal.requiredAttribute(OID,UUID::fromString))

    @GetMapping("enhet/{enhetsnummer}")
    @Operation(summary = "Hent alle medlemmer for en gitt enhet")
    fun medlemmerForEnhet(@PathVariable enhetsnummer: Enhetnummer) =
            medlemmer(enhetsnummer.gruppeNavn)

    @GetMapping("tema/{tema}")
    @Operation(summary = "Hent alle medlemmer for et gitt tema")
    fun medlemmerForTema(@PathVariable tema: Tema) =
            medlemmer(tema.gruppeNavn)

    @GetMapping("ansatt/{navIdent}")
    @Operation(summary = "Hent informasjon om ansatt ved bruk av NavIdent")
    fun utvidetAnsattForNavIdent(@PathVariable navIdent: AnsattId) =
        entraTjeneste.utvidetAnsatt(navIdent)

    @GetMapping("ansatt/tident/{tIdent}")
    @Operation(summary = "Hent informasjon om ansatt ved bruk av (AAA1234)")
    fun utvidetAnsattForTIdent(@PathVariable tIdent: TIdent) =
        entraTjeneste.utvidetAnsatt(tIdent)

    @GetMapping("/ansatt/tilganger/{navIdent}")
    @OAuth2RequireCCF
    @Operation(summary = "Hent informasjon om ansatts tilganger, krever CCFlow")
    fun grupperForAnsatt(@PathVariable navIdent: AnsattId) =
        oidTjeneste.ansattOid(navIdent)?.let {
            entraTjeneste.grupperForAnsatt(navIdent, it)
        }

    @GetMapping("gruppe/medlemmer")
    @Operation(summary = "Hent ansatte i en gitt gruppe")
    fun gruppeMedlemmer(gruppeNavn: String) =
        oidTjeneste.gruppeOid(gruppeNavn)?.let {
            entraTjeneste.medlemmerIGruppe( it)
        }

    private fun medlemmer(gruppeNavn: String) =
        oidTjeneste.gruppeOid(gruppeNavn)?.let {
            entraTjeneste.medlemmerIGruppe( it)
        } ?: emptySet()

    companion object {
        const val API_V1 = "/api/v1"
    }

}

fun <R> OAuth2AuthenticatedPrincipal.requiredAttribute(attributeName: String, mapper: (String) -> R): R =
    mapper(requireNotNull(getAttribute(attributeName)) { "Mangler $attributeName i OBO-token" })