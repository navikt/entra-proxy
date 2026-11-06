package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.felles.cache.CachableRestConfig
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.felles.rest.RestConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URI.create
import java.time.Duration
import java.time.Duration.ofHours
import java.time.Duration.ofSeconds

@Component
class NomGraphQLConfig(
    @Value($$"${nomgraph}") nomHost: String,
    @param:Value($$"${nom.varighet:3h}") override val varighet: Duration,
    @param:Value($$"${nom.lesetimeout:30s}") val lesetimeout: Duration) :
    CachableRestConfig, RestConfig(create("http://$nomHost$DEFAULT_GRAPHQL_PATH"), "", NOMGRAPH) {

    override val navn = name
    override val caches = NOM_CACHES

    companion object {
        const val NOMGRAPH = "nomgraph"
        val NOM_CACHE = CacheNøkkelConfig(NOMGRAPH)
        val NOM_CACHES = setOf(NOM_CACHE)
        private const val DEFAULT_GRAPHQL_PATH = "/graphql"
    }
}

