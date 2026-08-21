package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.AAD_ISSUER
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2RequireCCF
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2RequireOBO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

@ProdController
@Tag(name = "EntraController", description = "Denne kontrolleren skal brukes i produksjon")
class EntraController(private val entraTjeneste: EntraTjeneste,
                      private val oidTjeneste: EntraOidTjeneste,
                      private val token: Token) {

    @GetMapping("enhet/ansatt/{navIdent}")
    @OAuth2RequireCCF
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt, forutsetter CC-flow")
    fun enheterCC(@PathVariable navIdent: AnsattId) =
        hentForAnsatt(navIdent, entraTjeneste::enheter) { emptySet() }

    @GetMapping("enhet")
    @OAuth2RequireOBO
    @Operation(summary = "Hent alle tilgjengelige enheter for ansatt, forutsetter OBO-flow")
    fun enheterOBO() =
            entraTjeneste.enheter(token.ansattId!!, token.oid!!)
    @GetMapping("tema/ansatt/{navIdent}")
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt, forutsetter CC-flow")
    @OAuth2RequireCCF
    fun temaCC(@PathVariable navIdent: AnsattId) :Set<Tema> {
        val oid = oidTjeneste.ansattOid(navIdent)
         return  oid?.let {
           entraTjeneste.tema(navIdent,oid)
        } ?: emptySet()
    }
    @GetMapping("tema")
    @OAuth2RequireOBO
    @Operation(summary = "Hent alle tilgjengelige tema for ansatt, forutsetter OBO-flow")
    fun temaOBO() =
        entraTjeneste.tema(token.ansattId!!, token.oid!!)

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
            entraTjeneste.grupperForAnsatt(navIdent, it)
        }

    @GetMapping("gruppe/medlemmer")
    @Operation(summary = "Hent ansatte i en gitt gruppe")
    fun gruppeMedlemmer(gruppeNavn: String) =
        oidTjeneste.gruppeOid(gruppeNavn)?.let {
            entraTjeneste.medlemmer( it)
        }

    private inline fun <T> hentForAnsatt(navIdent: AnsattId, crossinline hent: (AnsattId, UUID) -> T, empty: () -> T) =
        oidTjeneste.ansattOid(navIdent)?.let { hent(navIdent, it) } ?: empty()

    private fun medlemmer(gruppeNavn: String) =
        oidTjeneste.gruppeOid(gruppeNavn)?.let {
            entraTjeneste.medlemmer( it)
        } ?: emptySet()

}

const val PROD_BASE_PATH = "/api/v1"

@Target(CLASS)
@Retention(RUNTIME)
@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@RestController
@RequestMapping(PROD_BASE_PATH)
@SecurityRequirement(name = "bearerAuth")
annotation class ProdController