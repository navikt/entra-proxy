package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.spring.ProtectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.AAD_ISSUER
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import java.util.UUID

@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@ProtectedRestController(value = ["/api/v1"], issuer = AAD_ISSUER, claimMap = [])
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "EntraController", description = "Denne kontrolleren skal brukes i produksjon")
class EntraController(private val entra: EntraTjeneste,
                      private val oid: AnsattOidTjeneste,
                      private val token: Token) {


    @PostMapping("enhet/ansatt/{ansattId}")
    @Operation(summary = "Slå opp enheter for ansatt, forutsetter CC-flow")
    fun enheterCC(@PathVariable ansattId: AnsattId) =
        token.assert({ erCC }, {
            hentForAnsatt(ansattId, entra::enheter) { emptySet() }
        })

    @PostMapping("enhet")
    @Operation(summary = "Slå opp enheter for ansatt, forutsetter OBO-flow")
    fun enheterOBO() = token.assert({ erObo }, {
        hentForObo(entra::enheter)
    })

    @PostMapping("tema/ansatt/{ansattId}")
    @Operation(summary = "Slå opp tema for ansatt, forutsetter CC-flow")
    fun temaCC(@PathVariable ansattId: AnsattId) =
        token.assert({ erCC }, {
            hentForAnsatt(ansattId, entra::tema) { emptySet() }
        })
    
    @PostMapping("tema")
    @Operation(summary = "Slå opp tema for ansatt, forutsetter OBO-flow")
    fun temaOBO() = token.assert( {erObo}, {
        hentForObo(entra::tema)
    })

    @PostMapping("enhet/{enhetsnummer}")
    @Operation(summary = "Slå opp medlemmer for enhet")
    fun medlemmer(@PathVariable enhetsnummer: Enhetnummer) =
            medlemmer(enhetsnummer.gruppeNavn)

    @PostMapping("tema/{tema}")
    @Operation(summary = "Slå opp medlemmer for tema")
    fun medlemmer(@PathVariable tema: Tema) =
            medlemmer(tema.gruppeNavn)

    private inline fun <T> hentForObo(hent: (AnsattId, UUID) -> T): T {
        val (ansattId, oid) = token.requireOboFields()
        return hent(ansattId, oid)
    }

    private inline fun <T> hentForAnsatt(ansattId: AnsattId, crossinline hent: (AnsattId, UUID) -> T, empty: () -> T): T =
        oid.oid(ansattId)?.let { hent(ansattId, it) } ?: empty()

    private fun medlemmer(gruppeNavn: String) =
        oid.gruppeId(gruppeNavn)?.let {
            entra.medlemmer( it)
        } ?: emptySet()


    fun Token.requireOboFields(): Pair<AnsattId, UUID> =
    ansattId?.let { id -> oid?.let { o -> id to o } }
    ?: error("ansattId og oid må være satt for OBO")

}
