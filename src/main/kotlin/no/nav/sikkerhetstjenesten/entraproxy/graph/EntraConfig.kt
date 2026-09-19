package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RestConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.ENTRA_PING_PATH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration

@Component
class EntraConfig(@Value($$"${spring.http.serviceclient.graph.base-url}") baseUri: URI,
                  @param:Value("\${graph.varighet:3h}") override val varighet : Duration) : CachableRestConfig, RestConfig(baseUri, ENTRA_PING_PATH, GRAPH) {
    override val navn = GRAPH
    override val caches = setOf(
        GRUPPER_FOR_ANSATT_GRAPH_CACHE,
        UTVIDET_ANSATT_GRAPH_CACHE,
        ENHETER_GRAPH_CACHE,
        TEMA_GRAPH_CACHE)

    companion object {
        const val TEMA = "tema"
        const val ENHETER = "enheter"
        const val UTVIDET_ANSATT = "utvidetAnsatt"
        const val GRUPPER_FOR_ANSATT = "grupperForAnsatt"
        val GRUPPER_FOR_ANSATT_GRAPH_CACHE = CacheNøkkelConfig(GRAPH,GRUPPER_FOR_ANSATT)
        val ENHETER_GRAPH_CACHE = CacheNøkkelConfig(GRAPH,ENHETER)
        val TEMA_GRAPH_CACHE = CacheNøkkelConfig(GRAPH,TEMA)
        val UTVIDET_ANSATT_GRAPH_CACHE = CacheNøkkelConfig(GRAPH,UTVIDET_ANSATT)
    }
}