package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOM_CACHE
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.felles.cache.getMany
import no.nav.sikkerhetstjenesten.felles.graphql.AbstractSyncGraphQLClientAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.graphql.client.GraphQlClient
import org.springframework.graphql.client.toEntityList
import org.springframework.stereotype.Component

@Component
class NomSyncGraphQLClientAdapter(cfg: NomGraphQLConfig, @Qualifier(NOMGRAPH) client: GraphQlClient, private val cache: CacheOperations) : AbstractSyncGraphQLClientAdapter(cfg, client), NomPort {

    override fun ansatt(ident: AnsattId): NomAnsatt =
        queryRequired<NomGraphQLRespons>(TILKNYTNINGER_QUERY, ident(ident.verdi)).toAnsatt()

    override fun ansatte(identer: Set<AnsattId>): Set<NomAnsatt>  {
        val fraCache = fraCache(identer)
        if (fraCache.size == identer.size) {
            log.trace("Hentet alle ${identer.size} ansatte fra CACHE")
            return fraCache.values.toSet()
        }
        // Hent de som ikke finnes i cache fra NOM
        val mangler = identer.filterNot { it.verdi in fraCache }
        log.trace("Henter ${mangler.size} ansatt(e) som ikke finnes i CACHE fra NOM")
        val fraNom = ressurser(BULK_TILKNYTNINGER_QUERY, identer(mangler.map { it.verdi }.toSet()))
        oppdaterCache(fraNom)
        return fraCache.values.toSet() + fraNom
    }

    override fun ansatteForEenhet(enhet: Enhetnummer): Set<NomAnsatt> {
        return ressurser(TILGANGSENHET_ANSATTE_QUERY, tilgangsenhet(enhet.verdi))

    }

    private fun ressurser(query: Pair<String, String>, variabler: Map<String, Any>): Set<NomAnsatt> =
        client.documentName(query.first)
            .variables(variabler)
            .retrieveSync(query.second)
            .toEntityList<NomBulkGraphQLRespons>()
            .mapNotNull { it.ressurs?.toAnsatt() }
            .toSet()

    private fun fraCache(identer: Set<AnsattId>) =
        cache.getMany<NomAnsatt>(NOM_CACHE, identer.map { it.verdi }.toSet())
            .filterValues { it != null }
            .mapValues { it.value!! }.also {
                log.trace("Hentet ${it.size} ansatte(er) av ${identer.size} mulige fra CACHE")
            }

    private fun oppdaterCache(ansatte: Set<NomAnsatt>) {
        if (ansatte.isEmpty()) return
        cache.putMany(NOM_CACHE, ansatte.associateBy { it.navident.verdi })
        log.trace("Oppdaterte CACHE med ${ansatte.size} ansatt(e) fra NOM")
    }

    private fun NomGraphQLRespons.toAnsatt() = NomAnsatt(navident, visningsnavn, gjeldendeSektor, orgTilknytninger)

    private companion object {
        private const val IDENT = "navident"
        private const val IDENTER = "navidenter"
        private const val TILGANGSENHET = "tilgangsenhetId"
        private val TILKNYTNINGER_QUERY = "orgtilknytninger" to "ressurs"
        private val BULK_TILKNYTNINGER_QUERY = "orgtilknytninger-bulk" to "ressurser"
        private val TILGANGSENHET_ANSATTE_QUERY = "ansatte-for-tilgangsenhet" to "ressurser"

        private fun ident(ident: String) = mapOf(IDENT to ident)
        private fun identer(identer: Set<String>) = mapOf(IDENTER to identer)
        private fun tilgangsenhet(enhetId: String) = mapOf(TILGANGSENHET to enhetId)



    }
}
