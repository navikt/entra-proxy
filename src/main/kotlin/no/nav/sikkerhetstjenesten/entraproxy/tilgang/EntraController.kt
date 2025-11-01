package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import hentForObo
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


    @PostMapping("CCF/ansatt/enheter/{ansattId}")
    @Operation(summary = "Slå opp enheter for ansatt, forutsetter CC-flow")
    fun enheterCC(@PathVariable ansattId: AnsattId) =
        token.assert({ erCC }, {
            hentForAnsatt(ansattId, entra::enheter) { emptySet() }
        })

    @PostMapping("CCF/ansatt/tema/{ansattId}")
    @Operation(summary = "Slå opp tema for ansatt, forutsetter CC-flow")
    fun temaCC(@PathVariable ansattId: AnsattId) =
        token.assert({ erCC }, {
            hentForAnsatt(ansattId, entra::tema) { emptySet() }
        })

    @PostMapping("ansatt/enheter")
    @Operation(summary = "Slå opp enheter for ansatt, forutsetter OBO-flow")
    fun enheterOBO() = token.assert({ erObo }, {
        hentForObo(entra::enheter)
    })
    @PostMapping("ansatt/tema")
    @Operation(summary = "Slå opp tema for ansatt, forutsetter OBO-flow")
    fun temaOBO() = token.assert( {erObo}, {
        hentForObo(entra::tema)
    })

    @PostMapping("CCF/enheter/medlemmer/{enhetsnummer}")
    @Operation(summary = "Slå opp medlemmer for enhet, forutsetter CC-flow")
    fun enhetMedlemmerCC(@PathVariable enhetsnummer: Enhetnummer) =
            medlemmer({erCC},enhetsnummer.gruppeNavn)

    @PostMapping("CCF/tema/medlemmer/{tema}")
    @Operation(summary = "Slå opp medlemmer for tema, forutsetter CC-flow")
    fun temaMedlemmerCC(@PathVariable tema: Tema) =
            medlemmer({erCC},tema.gruppeNavn)

    @PostMapping("enheter/medlemmer/{enhetsnummer}")
    @Operation(summary = "Slå opp medlemmer for enhet, forutsetter Obo-flow")
    fun enhetMedlemmerOBO(@PathVariable enhetsnummer: Enhetnummer) =
            medlemmer({erObo},enhetsnummer.gruppeNavn)

    @PostMapping("tema/medlemmer/{tema}")
    @Operation(summary = "Slå opp medlemmer for tema, forutsetter Obo-flow")
    fun temaMedlemmerOBO(@PathVariable tema: Tema) =
            medlemmer({erObo},tema.gruppeNavn)

    private inline fun <T> hentForObo(hent: (AnsattId, UUID) -> T): T {
        val (ansattId, oid) = token.requireOboFields()
        return hent(ansattId, oid)
    }
    private inline fun <T> hentForAnsatt(ansattId: AnsattId, crossinline hent: (AnsattId, UUID) -> T, empty: () -> T): T =
        oid.oid(ansattId)?.let { hent(ansattId, it) } ?: empty()

    private fun medlemmer(predikat: Token.() -> Boolean,gruppeNavn: String) =
        token.assert( predikat, {
            entra.gruppeId(gruppeNavn)?.let {
                entra.medlemmer( it)
            } ?: emptySet()
        })


    fun Token.requireOboFields(): Pair<AnsattId, UUID> =
    ansattId?.let { id -> oid?.let { o -> id to o } }
    ?: error("ansattId og oid må være satt for OBO")

}
