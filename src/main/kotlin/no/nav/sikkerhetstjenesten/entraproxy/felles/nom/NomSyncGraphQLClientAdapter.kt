package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.felles.graphql.AbstractSyncGraphQLClientAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.graphql.client.GraphQlClient
import org.springframework.stereotype.Component

@Component
class NomSyncGraphQLClientAdapter(cfg: NomGraphQLConfig, @Qualifier(NomGraphQLConfig.NOMGRAPH) client: GraphQlClient) : AbstractSyncGraphQLClientAdapter(cfg, client) {

    fun orgTilknytninger(ansattId: String) = query<NomRespons>(TILKNYTNINGER_QUERY, ident(ansattId))

    companion object {
        private const val IDENT = "navident"
        private fun ident(navident: String) = mapOf(IDENT to navident)
        private val TILKNYTNINGER_QUERY = "orgtilknytninger" to "ressurs"
    }
}

