package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.security.token.support.spring.UnprotectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.DEV
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhetsnummer
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.client.RestClient

@UnprotectedRestController(value = ["/${DEV}"])
@ConditionalOnNotProd
@Tag(name = "DevTilgangController", description = "Denne kontrolleren skal kun brukes til testing")
class DevEntraController (private val entra: EntraTjeneste, private val oid: AnsattOidTjeneste, private val norgTjeneste: NorgTjeneste) {

    @GetMapping("ansatt/enheter/{ansattId}")
    fun enheter(@PathVariable ansattId: AnsattId) = entra.enheter(ansattId, oid.oidFraEntra(ansattId))

    @GetMapping("ansatt/tema/{ansattId}")
    fun tema(@PathVariable ansattId: AnsattId) = entra.tema(ansattId, oid.oidFraEntra(ansattId))

    @GetMapping("ansatt/tema/{enhetsnummer}")
    fun enhetNavn(@PathVariable enhetsnummer: Enhetsnummer) = norgTjeneste.navnFor(enhetsnummer)
}