package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.ENTRA_PING_PATH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerCachableRestConfig.Companion.MEDLEMMER
import org.springframework.beans.factory.annotation.Value
import java.net.URI
import java.time.Duration

class EntraConfig(@Value($$"${spring.http.serviceclient.graph.base-url}") baseUri: URI,
                  override val varighet : Duration) : CachableRestConfig, AbstractRestConfig(baseUri, ENTRA_PING_PATH, GRAPH) {
    override val navn = GRAPH
    override val caches = setOf(OID_CACHE,MEDLEMMER_CACHE)

    companion object {
        const val MEDLEMMER = "medlemmer"
        const val ENTRA_OID = "entraoid"
        val OID_CACHE = CacheNøkkelConfig(ENTRA_OID)
        val MEDLEMMER_CACHE = CacheNøkkelConfig(MEDLEMMER)
    }
}