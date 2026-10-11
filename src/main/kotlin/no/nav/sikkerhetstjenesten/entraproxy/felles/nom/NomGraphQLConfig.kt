package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.felles.rest.RestConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI

@Component
class NomGraphQLConfig(@Value($$"${nomgraph}") nomHost: String) : RestConfig(URI.create("http://$nomHost$DEFAULT_GRAPHQL_PATH"), "", NOMGRAPH) {

    companion object {
        const val NOMGRAPH = "nomgraph"
        private const val DEFAULT_GRAPHQL_PATH = "/graphql"
    }
}

