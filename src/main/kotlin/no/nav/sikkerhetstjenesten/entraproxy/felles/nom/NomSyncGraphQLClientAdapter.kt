package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.felles.graphql.AbstractSyncGraphQLClientAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.graphql.client.GraphQlClient
import org.springframework.graphql.client.toEntityList
import org.springframework.stereotype.Component

@Component
class NomSyncGraphQLClientAdapter(cfg: NomGraphQLConfig, @Qualifier(NomGraphQLConfig.NOMGRAPH) client: GraphQlClient) : AbstractSyncGraphQLClientAdapter(cfg, client), NomPort {

    override fun ansatt(ident: AnsattId): NomAnsatt =
        queryRequired<NomGraphQLRespons>(TILKNYTNINGER_QUERY, ident(ident.verdi)).toAnsatt()

    override fun ansatte(identer: Set<AnsattId>): Set<NomAnsatt> =
        hentRessurser(BULK_TILKNYTNINGER_QUERY, identer(identer.map { it.verdi }.toSet()))

    override fun ansatteForTilgangsenhet(tilgangsenhetId: Enhetnummer): Set<NomAnsatt> =
        hentRessurser(TILGANGSENHET_ANSATTE_QUERY, tilgangsenhet(tilgangsenhetId.verdi))

    private fun hentRessurser(query: Pair<String, String>, variabler: Map<String, Any>): Set<NomAnsatt> =
        client.documentName(query.first)
            .variables(variabler)
            .retrieveSync(query.second)
            .toEntityList<NomBulkGraphQLRespons>()
            .mapNotNull { it.ressurs?.toAnsatt() }
            .toSet()

    private fun NomGraphQLRespons.toAnsatt() = NomAnsatt(navident, visningsnavn, gjeldendeSektor, orgTilknytninger)

    private companion object {
        private const val IDENT = "navident"
        private const val IDENTER = "navidenter"
        private const val TILGANGSENHET = "tilgangsenhetId"
        private val TILKNYTNINGER_QUERY = "orgtilknytninger" to "ressurs"
        private val BULK_TILKNYTNINGER_QUERY = "orgtilknytninger-bulk" to "ressurser"
        private val TILGANGSENHET_ANSATTE_QUERY = "ansatte-for-tilgangsenhet" to "ressurser"

        private fun ident(navident: String) = mapOf(IDENT to navident)
        private fun identer(navidenter: Set<String>) = mapOf(IDENTER to navidenter)
        private fun tilgangsenhet(tilgangsenhetId: String) = mapOf(TILGANGSENHET to tilgangsenhetId)



    }
}
