package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.security.token.support.spring.UnprotectedRestController
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.DEV
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@UnprotectedRestController(value = ["/${DEV}"])
@ConditionalOnNotProd
@Tag(name = "DevEntraController", description = "Denne kontrolleren skal kun brukes til testing")
class DevEntraController (private val entraTjeneste: EntraTjeneste, private val oidTjeneste: AnsattOidTjeneste, private val adapter: EntraRestClientAdapter) {

    @GetMapping("ansatt/enheter/{ansattId}")
    fun enheter(@PathVariable ansattId: AnsattId) = oidTjeneste.oid(ansattId).let { entraTjeneste.enheter(ansattId, it) }

    @GetMapping("ansatt/temaer/{ansattId}")
    fun temaer(@PathVariable ansattId: AnsattId) = oidTjeneste.oid(ansattId).let { entraTjeneste.tema(ansattId, it) }

     @GetMapping("enheter/{enhetsnummer}/medlemmer")
    fun enhetMedlemmer(@PathVariable enhetsnummer: Enhetnummer) =   gruppeIdForEnhet(enhetsnummer)?.let {
         entraTjeneste.medlemmer( it)
     }
    @GetMapping("tema/{tema}/medlemmer")
    fun temaMedlemmer(@PathVariable tema: Tema) =   gruppeIdForTema(tema)?.let {
        entraTjeneste.medlemmer( it)
    }

    @GetMapping("medlemmer/{oid}")
    fun medlemmer(@PathVariable oid: UUID) = adapter.medlemmer(oid.toString())

    @GetMapping("medlemmer/any/{oid}")
    fun medlemmerAny(@PathVariable oid: UUID) = adapter.medlemmerAny(oid.toString())

    @GetMapping("medlemmer/any1/{oid}")
    fun medlemmerAny1(@PathVariable oid: UUID) = adapter.medlemmerAny1(oid.toString())


    @GetMapping("enhet/{enhetsnummer}")
    fun gruppeIdForEnhet(@PathVariable enhetsnummer: Enhetnummer) = entraTjeneste.gruppeIdForEnhet(enhetsnummer)

    @GetMapping("tema/{tema}")
    fun gruppeIdForTema(@PathVariable tema: Tema) = entraTjeneste.gruppeIdForTema(tema)

    @GetMapping("ansatt/{ansattId}")
    fun oid(@PathVariable ansattId: AnsattId) = oidTjeneste.oid(ansattId)
}