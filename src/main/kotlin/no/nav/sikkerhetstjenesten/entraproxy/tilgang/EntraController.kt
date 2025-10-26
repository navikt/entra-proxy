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
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@ProtectedRestController(value = ["/api/v1"], issuer = AAD_ISSUER, claimMap = [])
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "EntraController", description = "Denne kontrolleren skal brukes i produksjon")
class EntraController(private val entra: EntraTjeneste,
                      private val oid: AnsattOidTjeneste,
                      private val token: Token) {


    @GetMapping("ansatt/enheter/{ansattId}")
    @Operation(summary = "Slå opp enheter for ansatt, forutsetter CC-flow")
    fun enheterCC(@PathVariable ansattId: AnsattId) =
        tokenPrecondition( {token.erCC}, {
            oid.oidFraEntra(ansattId)?.let { entra.enheter(ansattId, it) } ?: emptySet<Enhet>()
        })

    @GetMapping("ansatt/tema/{ansattId}")
    @Operation(summary = "Slå opp tema for ansatt, forutsetter CC-flow")
    fun temaCC(@PathVariable ansattId: AnsattId) =
        tokenPrecondition( {token.erCC}, {
            oid.oidFraEntra(ansattId)?.let { entra.tema(ansattId, it) } ?: emptySet<Tema>()
        })

    @PostMapping("ansatt/enheter")
    @Operation(summary = "Slå opp enheter for ansatt, forutsetter OBO-flow")
    fun enheterOBO() = tokenPrecondition( {token.erObo}, {
        entra.enheter(token.ansattId!!, token.oid!!)
    })
    @PostMapping("ansatt/tema")
    @Operation(summary = "Slå opp tema for ansatt, forutsetter OBO-flow")
    fun temaOBO() = tokenPrecondition( {token.erObo}, {
        entra.tema(token.ansattId!!, token.oid!!)
    })
    
    private fun tokenPrecondition(predikat: () -> Boolean, block: () -> Any) {
        if (!predikat()) throw ResponseStatusException(BAD_REQUEST, "Feil i token: krever korrekt token-type for å utføre denne operasjonen")
        else block()
    }
}

