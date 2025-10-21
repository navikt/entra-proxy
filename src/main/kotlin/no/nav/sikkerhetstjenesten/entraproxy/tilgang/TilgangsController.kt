package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.micrometer.core.instrument.Tags
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import no.nav.security.token.support.spring.ProtectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.AnsattOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.AnsattTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.teller.TokenTypeTeller
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.Token.Companion.AAD_ISSUER
import org.jboss.logging.MDC
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException

@SecurityScheme(bearerFormat = "JWT", name = "bearerAuth", scheme = "bearer", type = HTTP)
@ProtectedRestController(value = ["/api/v1"], issuer = AAD_ISSUER, claimMap = [])
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "TilgangController", description = "Denne kontrolleren skal brukes i produksjon")
class TilgangsController(private val token: Token,
                         private val teller: TokenTypeTeller,
                         private val entra: EntraTjeneste,
                         private val oid: AnsattOidTjeneste,
                         private val ansatte: AnsattTjeneste
) {

    private val log = getLogger(javaClass)

    @GetMapping("ansatt/enheter/{ansattId}")
    fun enheter(@PathVariable ansattId: AnsattId) = entra.geoOgGlobaleGrupper(ansattId, oid.oidFraEntra(ansattId)).filter { it.displayName.contains("ENHET") }

    @GetMapping("ansatt/tema/{ansattId}")
    fun tema(@PathVariable ansattId: AnsattId) = entra.tema(ansattId, oid.oidFraEntra(ansattId))
}