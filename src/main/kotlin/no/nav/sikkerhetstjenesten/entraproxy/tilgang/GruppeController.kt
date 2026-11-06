package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppeTjeneste
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "GruppeController", description = "Disse endepunktene skal brukes i produksjon")
@RestController
@RequestMapping(API_V1)
class GruppeController(private val gruppe: EntraGruppeTjeneste, private val oid: EntraOidTjeneste) {

    @GetMapping("gruppe/medlemmer")
    @Operation(summary = "Hent ansatte i en gitt gruppe fra Entra")
    fun gruppeMedlemmer(@RequestParam gruppeNavn: String) =
        medlemmerIGruppe(oid, gruppe, gruppeNavn)

    @GetMapping("gruppe/medlemmer/sok")
    @Operation(summary = "Hent ansatte i alle grupper som matcher en substring av gruppenavnet fra Entra")
    fun medlemmerIGrupperSomMatcher(@RequestParam gruppeNavn: String) =
        gruppe.medlemmerIGrupperSomMatcher(gruppeNavn)

    @GetMapping("gruppe/medlemmer/sok/per-ansatt")
    @Operation(summary = "Hent hvilke matchende grupper hver ansatt er medlem i, for grupper som matcher en substring av gruppenavnet")
    fun grupperPerAnsattSomMatcher(@RequestParam gruppeNavn: String) =
        gruppe.grupperPerAnsattSomMatcher(gruppeNavn)

    @PostMapping("gruppe/antall/bulk")
    @Operation(summary = "Hent antall medlemmer for flere grupper fra Entra")
    fun antallMedlemmerIGrupper(@RequestBody gruppeIder: Set<UUID>) =
        gruppe.antallMedlemmerIGrupper(*gruppeIder.toTypedArray())
}
