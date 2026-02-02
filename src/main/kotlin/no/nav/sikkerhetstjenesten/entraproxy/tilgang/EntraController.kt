package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.spring.ProtectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.AAD_ISSUER
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID
@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@ProtectedRestController(value = ["/api/v1"], issuer = AAD_ISSUER, claimMap = [])
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "EntraController", description = "Denne kontrolleren skal brukes i produksjon")
class EntraController(private val entraTjeneste: EntraTjeneste,
                      private val oidTjeneste: EntraOidTjeneste,
                      private val token: Token) {

    @GetMapping("enhet/ansatt/{navIdent}")
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt, forutsetter CC-flow")
    fun enheterCC(@PathVariable navIdent: AnsattId) =
        token.assert({ erCC }, {
            hentForAnsatt(navIdent, entraTjeneste::enheter) { emptySet() }
        })

    @GetMapping("enhet")
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt, forutsetter OBO-flow")
    fun enheterOBO() =
        token.assert({ erObo }, {
            hentForObo(entraTjeneste::enheter)
        })

    @GetMapping("tema/ansatt/{navIdent}")
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt, forutsetter CC-flow")
    fun temaCC(@PathVariable navIdent: AnsattId) =
        token.assert({ erCC }, {
            hentForAnsatt(navIdent, entraTjeneste::tema) { emptySet() }
        })

    @GetMapping("tema")
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt, forutsetter OBO-flow")
    fun temaOBO() =
        token.assert( {erObo}, {
            hentForObo(entraTjeneste::tema)
        })

    @GetMapping("enhet/{enhetsnummer}")
    @Operation(summary = "Hent alle medlemmer for en gitt enhet")
    fun medlemmer(@PathVariable enhetsnummer: Enhetnummer) =
            medlemmer(enhetsnummer.gruppeNavn)

    @GetMapping("tema/{tema}")
    @Operation(summary = "Hent alle medlemmer for et gitt tema")
    fun medlemmer(@PathVariable tema: Tema) =
            medlemmer(tema.gruppeNavn)

    @GetMapping("ansatt/{navIdent}")
    @Operation(summary = "Hent informasjon om ansatt ved bruk av NavIdent")
    fun utvidetAnsatt(@PathVariable navIdent: AnsattId) =
        entraTjeneste.utvidetAnsatt(navIdent)

    @GetMapping("ansatt/tident/{tIdent}")
    @Operation(summary = "Hent informasjon om ansatt ved bruk av (AAA1234")
    fun utvidetAnsatt(@PathVariable tIdent: TIdent) =
        entraTjeneste.utvidetAnsatt(tIdent)

    @GetMapping("/ansatt/tilganger/{navIdent}")
    @Operation(summary = "Hent informasjon om ansatts tilganger, krever CCFlow")
    fun grupperForAnsatt(@PathVariable navIdent: AnsattId) =
        oidTjeneste.ansattOid(navIdent)?.let {
            entraTjeneste.grupperForAnsatt( it, navIdent)
        }
    
    @GetMapping("gruppe/medlemmer")
    @Operation(summary = "Hent ansatte i en gitt gruppe")
    fun gruppeMedlemmer(gruppeNavn: String) =
        oidTjeneste.gruppeOid(gruppeNavn)?.let {
            entraTjeneste.medlemmer( it)
        }


    private inline fun <T> hentForObo(hent: (AnsattId, UUID) -> T) =
        with(token.oboFields) {
            hent(first, second)
        }

    private inline fun <T> hentForAnsatt(navIdent: AnsattId, crossinline hent: (AnsattId, UUID) -> T, empty: () -> T) =
        oidTjeneste.ansattOid(navIdent)?.let { hent(navIdent, it) } ?: empty()

    private fun medlemmer(gruppeNavn: String) =
        oidTjeneste.gruppeOid(gruppeNavn)?.let {
            entraTjeneste.medlemmer( it)
        } ?: emptySet()

}
