package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.security.token.support.spring.UnprotectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.DEV
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@UnprotectedRestController(value = ["/${DEV}"])
@ConditionalOnNotProd
@Tag(name = "DevEntraController", description = "Denne kontrolleren skal kun brukes til testing")
class DevEntraController (private val entra: EntraTjeneste, private val oid: AnsattOidTjeneste) {

    @GetMapping("ansatt/enheter/{ansattId}")

    fun enheter(@PathVariable ansattId: AnsattId) = oid.oidFraEntra(ansattId)?.let { entra.enheter(ansattId, it) } ?: emptySet()

    @GetMapping("ansatt/tema/{ansattId}")
    fun tema(@PathVariable ansattId: AnsattId) = oid.oidFraEntra(ansattId)?.let { entra.tema(ansattId, it) } ?: emptySet()
}