package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.API_V1
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "AnsattController", description = "Disse endepunktene skal brukes i produksjon")
@RestController
@RequestMapping(API_V1)
class AnsattController(private val entra: EntraTjeneste) {

    @GetMapping("ansatt/{navIdent}")
    @Operation(summary = "Hent informasjon om ansatt fra Entra")
    fun utvidetAnsattForNavIdent(@PathVariable navIdent: AnsattId) =
        entra.utvidetAnsatt(navIdent)

    @GetMapping("ansatt/tident/{tIdent}")
    @Operation(summary = "Hent informasjon om en ansatt fra Entra fot TIdent")
    fun utvidetAnsattForTIdent(@PathVariable tIdent: TIdent) =
        entra.utvidetAnsatt(tIdent)

    @GetMapping("/ansatt/tilganger/{navIdent}")
    @Operation(summary = "Hent informasjon om den ansattes tilganger fra Entra")
    fun grupperForAnsatt(@PathVariable navIdent: AnsattId) =
        entra.grupper(navIdent)
}
