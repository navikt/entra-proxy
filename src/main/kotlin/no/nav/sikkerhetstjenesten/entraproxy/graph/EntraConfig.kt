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
    override val caches = setOf(GRUPPER_FOR_ANSATT_GRAPH_CACHE,UTVIDET_ANSATT_GRAPH_CACHE, ENHETER_GRAPH_CACHE,TEMA_GRAPH_CACHE)

    companion object {
        val GRUPPER_FOR_ANSATT_GRAPH_CACHE = CacheNøkkelConfig(GRAPH,"grupperForAnsatt")
        val ENHETER_GRAPH_CACHE = CacheNøkkelConfig(GRAPH,"enheter")
        val TEMA_GRAPH_CACHE = CacheNøkkelConfig(GRAPH,"tema")
        val UTVIDET_ANSATT_GRAPH_CACHE = CacheNøkkelConfig(GRAPH,"utvidetAnsatt")
    }
}