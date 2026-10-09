package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.felles.graphql.AbstractSyncGraphQLClientAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.graphql.client.GraphQlClient
import org.springframework.graphql.client.toEntityList
import org.springframework.stereotype.Component

@Component
class NomSyncGraphQLClientAdapter(cfg: NomGraphQLConfig, @Qualifier(NomGraphQLConfig.NOMGRAPH) client: GraphQlClient) : AbstractSyncGraphQLClientAdapter(cfg, client) {

    fun orgData(ident: String) = queryRequired<NomGraphQLRespons>(TILKNYTNINGER_QUERY, ident(ident))

    fun orgDataBulk(identer: Set<String>): Set<NomGraphQLRespons> {
        return client.documentName(BULK_TILKNYTNINGER_QUERY.first)
            .variables(identer(identer))
            .retrieveSync(BULK_TILKNYTNINGER_QUERY.second)
            .toEntityList<NomBulkGraphQLRespons>().mapNotNull { it.ressurs }.toSet()
    }

    companion object {
        private const val IDENT = "navident"
        private const val IDENTER = "navidenter"

        private fun ident(navident: String) = mapOf(IDENT to navident)
        private fun identer(navidenter: Set<String>) = mapOf(IDENTER to navidenter)

        private val TILKNYTNINGER_QUERY = "orgtilknytninger" to "ressurs"
        private val BULK_TILKNYTNINGER_QUERY = "orgtilknytninger-bulk" to "ressurser"

    }
}
