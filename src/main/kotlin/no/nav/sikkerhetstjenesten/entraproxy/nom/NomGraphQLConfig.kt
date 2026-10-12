package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.felles.cache.CachableRestConfig
import no.nav.sikkerhetstjenesten.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.felles.rest.RestConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Duration

@Component
class NomGraphQLConfig(@Value($$"${nomgraph}") nomHost: String,
                       @param:Value($$"${nom.varighet:3h}") override val varighet: Duration = Duration.ofHours(3)) :
    CachableRestConfig, RestConfig(URI.create("http://$nomHost$DEFAULT_GRAPHQL_PATH"), "", NOMGRAPH) {

    override val navn = name
    override val caches = setOf(NOM_CACHE)

    companion object {
        const val NOMGRAPH = "nomgraph"
        val NOM_CACHE = CacheNøkkelConfig(NOMGRAPH)
        private const val DEFAULT_GRAPHQL_PATH = "/graphql"
    }
}

