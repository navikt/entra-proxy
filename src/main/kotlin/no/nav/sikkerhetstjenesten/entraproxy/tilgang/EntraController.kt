package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.spring.ProtectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.AnsattOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.Token.Companion.AAD_ISSUER
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@ProtectedRestController(value = ["/api/v1"], issuer = AAD_ISSUER, claimMap = [])
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "TilgangController", description = "Denne kontrolleren skal brukes i produksjon")
class EntraController(private val entra: EntraTjeneste,
                      private val oid: AnsattOidTjeneste,
                      private val token: Token) {


    @GetMapping("ansatt/enheter/{ansattId}")
    @Operation(summary = "Slå opp enheter for ansatt, forutsetter CC-flow")
    fun enheterCC(@PathVariable ansattId: AnsattId) =
        tokenPrecondition( {token.erCC}, {
            entra.geoOgGlobaleGrupper(ansattId, oid.oidFraEntra(ansattId)).filter { it.displayName.contains("ENHET") }
        })

    @GetMapping("ansatt/tema/{ansattId}")
    @Operation(summary = "Slå opp tema for ansatt, forutsetter CC-flow")
    fun temaCC(@PathVariable ansattId: AnsattId) =
        tokenPrecondition( {token.erCC}, {
            entra.tema(ansattId, oid.oidFraEntra(ansattId))
        })

    @PostMapping("ansatt/enheter")
    @ProblemDetailApiResponse
    @Operation(summary = "Slå opp enheter for ansatt, forutsetter OBO-flow")
    fun enheterOBO() = tokenPrecondition( {token.erObo}, {
        entra.geoOgGlobaleGrupper(token.ansattId!!, token.oid!!).filter { it.displayName.contains("ENHET") }
    })
    @PostMapping("ansatt/tema")
    @ProblemDetailApiResponse
    @Operation(summary = "Slå opp tema for ansatt, forutsetter OBO-flow")
    fun temaOBO() = tokenPrecondition( {token.erObo}, {
        entra.tema(token.ansattId!!, token.oid!!)
    })
    
    private fun tokenPrecondition(predikat: () -> Boolean, block: () -> Any) {
        if (!predikat()) throw ResponseStatusException(BAD_REQUEST, "Feil i token: krever korrekt token-type for å utføre denne operasjonen")
        else block()
    }
}

annotation class ProblemDetailApiResponse
@Schema(description = "Problem Detail")
internal data class ProblemDetailResponse(
    val type: URI,
    val status: Int,
    val instance: String,
    val navIdent: String,
    val traceId: String)