package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.felles.rest.PingableHealthIndicator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.graphql.client.GraphQlClient
import org.springframework.stereotype.Component

@Component
class NomHealthIndicator(cfg: NomGraphQLConfig, @Qualifier(NOMGRAPH) client: GraphQlClient) :
    HealthIndicator by PingableHealthIndicator(cfg, {
        client.document(PING_QUERY).executeSync()
    }) {

    private companion object {
        private const val PING_QUERY = "{ __typename }"
    }
}
