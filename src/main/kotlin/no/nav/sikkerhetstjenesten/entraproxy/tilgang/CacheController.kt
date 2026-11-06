package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.EntraProxyApplication.Companion.API_V1
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "CacheController", description = "Disse endepunktene skal brukes i produksjon, men normalt ikke kalles direkte")
@RestController
@RequestMapping(API_V1)
class CacheController(private val cache: CacheOperations) {

    @PostMapping("cache/flushAll")
    @Operation(summary = "Tøm alle caches")
    fun flushAll() = cache.clear()

    @PostMapping("cache/flush")
    @Operation(summary = "Tøm cache")
    fun flush() = cache.clear(NOMGRAPH, GRAPH, MEDLEMMER)
}
