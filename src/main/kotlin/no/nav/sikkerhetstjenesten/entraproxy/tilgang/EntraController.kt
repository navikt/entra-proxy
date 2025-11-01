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
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping

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
        token.precondition( {erCC}, {
            oid.oid(ansattId)?.let { entra.enheter(ansattId, it) } ?: emptySet<Enhet>()
        })

    @PostMapping("CCF/ansatt/tema/{ansattId}")
    @Operation(summary = "Slå opp tema for ansatt, forutsetter CC-flow")
    fun temaCC(@PathVariable ansattId: AnsattId) =
        token.precondition( {erCC}, {
            oid.oid(ansattId)?.let { entra.tema(ansattId, it) } ?: emptySet<Tema>()
        })

    @PostMapping("ansatt/enheter")
    @Operation(summary = "Slå opp enheter for ansatt, forutsetter OBO-flow")
    fun enheterOBO() = token.precondition( {erObo}, {
        entra.enheter(token.ansattId!!, token.oid!!)
    })
    @PostMapping("ansatt/tema")
    @Operation(summary = "Slå opp tema for ansatt, forutsetter OBO-flow")
    fun temaOBO() = token.precondition( {erObo}, {
        entra.tema(token.ansattId!!, token.oid!!)
    })

    @PostMapping("CCF/enheter/medlemmer/{enhetsnummer}")
    @Operation(summary = "Slå opp medlemmer for enhet, forutsetter CC-flow")
    fun enhetMedlemmerCC(@PathVariable enhetsnummer: Enhetnummer) =
        token.precondition( {erCC}, {
            medlemmer(enhetsnummer.gruppeNavn)
        })

    @PostMapping("CCF/tema/medlemmer/{tema}")
    @Operation(summary = "Slå opp medlemmer for tema, forutsetter CC-flow")
    fun temaMedlemmerCC(@PathVariable tema: Tema) =
        token.precondition( {erCC}, {
            medlemmer(tema.gruppeNavn)
        })


    @PostMapping("enheter/medlemmer/{enhetsnummer}")
    @Operation(summary = "Slå opp medlemmer for enhet, forutsetter Obo-flow")
    fun enhetMedlemmerOBO(@PathVariable enhetsnummer: Enhetnummer) =
        token.precondition( {erObo}, {
            medlemmer(enhetsnummer.gruppeNavn)
        })

    @PostMapping("tema/medlemmer/{tema}")
    @Operation(summary = "Slå opp medlemmer for tema, forutsetter Obo-flow")
    fun temaMedlemmerOBO(@PathVariable tema: Tema) =
        token.precondition( {erObo}, {
            medlemmer(tema.gruppeNavn)
        })

    private fun medlemmer(gruppeNavn: String) =
        entra.gruppeId(gruppeNavn)?.let {
            entra.medlemmer( it)
        } ?: emptySet()
    

}

