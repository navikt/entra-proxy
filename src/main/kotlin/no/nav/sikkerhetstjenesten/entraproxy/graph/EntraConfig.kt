package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import org.springframework.beans.factory.annotation.Value
import java.net.URI
import java.time.Duration

class EntraConfig(@Value("\${spring.http.serviceclient.graph.base-url}") baseUri: URI,
    override val varighet : Duration) : CachableRestConfig, AbstractRestConfig(baseUri, pingPath, GRAPH)