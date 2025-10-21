package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.security.token.support.spring.UnprotectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.AnsattOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheClient
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.DEV
import org.slf4j.LoggerFactory.getLogger

@UnprotectedRestController(value = ["/${DEV}"])
@ConditionalOnNotProd
@Tag(name = "DevTilgangController", description = "Denne kontrolleren skal kun brukes til testing")
class DevTilgangController (
    private val entra: EntraTjeneste,
    private val oid: AnsattOidTjeneste,
    private val cache: CacheClient) {

    private val log = getLogger(javaClass)

    @GetMapping("ansatt/enheter/{ansattId}")
    fun enheter(@PathVariable ansattId: AnsattId) = entra.geoOgGlobaleGrupper(ansattId, oid.oidFraEntra(ansattId)).filter { it.displayName.contains("ENHET") }

    @GetMapping("ansatt/tema/{ansattId}")
    fun tema(@PathVariable ansattId: AnsattId) = entra.tema(ansattId, oid.oidFraEntra(ansattId))
}