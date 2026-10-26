package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOM_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet
import no.nav.sikkerhetstjenesten.entraproxy.nom.NomOrgTilknytning.NomEnhet.NomIdent
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.felles.cache.getMany
import no.nav.sikkerhetstjenesten.felles.graphql.AbstractSyncGraphQLClientAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.graphql.client.GraphQlClient
import org.springframework.graphql.client.toEntity
import org.springframework.graphql.client.toEntityList
import org.springframework.stereotype.Component

@Component
class NomSyncGraphQLClientAdapter(
    cfg: NomGraphQLConfig,
    @Qualifier(NOMGRAPH) client: GraphQlClient,
    private val cache: CacheOperations) : AbstractSyncGraphQLClientAdapter(cfg, client), NomOperations {

    override fun ansatt(ident: AnsattId): NomAnsatt? =
        query<NomGraphQLRespons>(TILKNYTNINGER_QUERY, ident(ident.verdi))?.toAnsatt()?.kunDagligOppfolging()

    override fun ansatte(identer: Set<AnsattId>): Set<NomAnsatt> {
        val fraCache = fraCache(identer)
        if (fraCache.size == identer.size) {
            log.trace("Hentet alle ${identer.size} ansatte fra CACHE")
            return fraCache.values.toSet()
        }
        val mangler = identer.filterNotTo(mutableSetOf()) { it.verdi in fraCache }.map { it.verdi }.toSet()
        log.trace("Henter ${mangler.size} ansatt(e) som ikke finnes i CACHE fra NOM")
        val fraNom = ressurser(BULK_TILKNYTNINGER_QUERY, identer(mangler))
            .mapTo(mutableSetOf()) { it.kunDagligOppfolging() }
        oppdaterCache(fraNom)
        return fraCache.values.toSet() + fraNom
    }

    override fun ansatteForEnhet(enhet: Enhetnummer) =
        ressurser(TILGANGSENHET_ANSATTE_QUERY, tilgangsenhet(enhet.verdi))
            .filterTo(mutableSetOf()) { ansatt ->
                ansatt.orgTilknytninger.any { it.orgEnhet.tilgangsenhetId == enhet && it.erDagligOppfolging }
            }

    override fun ansatteForNomIdent(ident: NomIdent): NomEnhetAnsatte {
        val respons = client.documentName(NOMID_ANSATTE_QUERY.first)
            .variables(nomident(ident.verdi))
            .executeSync()
        val enhet = respons.field(NOMID_ANSATTE_TOPPNODE).toEntity<NomEnhet>()
        val ansatte = respons.field(NOMID_ANSATTE_QUERY.second)
            .toEntityList<NomOrgEnhetRespons>()
            .flatMap { it.orgEnhet?.orgTilknytninger.orEmpty() }
            .filter { it.erDagligOppfolging }
            .mapNotNullTo(mutableSetOf()) { it.ressurs?.toAnsatt() }
        return NomEnhetAnsatte(ident, enhet?.navn, ansatte)
    }

    private fun ressurser(query: Pair<String, String>, variabler: Map<String, Any>) =
        hentListe<NomBulkGraphQLRespons>(query, variabler)
            .mapNotNullTo(mutableSetOf()) { it.ressurs?.toAnsatt() }

    private inline fun <reified T : Any> hentListe(query: Pair<String, String>, variabler: Map<String, Any>): List<T> =
        client.documentName(query.first)
            .variables(variabler)
            .retrieveSync(query.second)
            .toEntityList<T>()

    private fun fraCache(identer: Set<AnsattId>) =
        cache.getMany<NomAnsatt>(NOM_CACHE, identer.mapTo(mutableSetOf()) { it.verdi })
            .filterValues { it != null }
            .mapValues { it.value!! }
            .also { log.trace("Hentet ${it.size} ansatte(er) av ${identer.size} mulige fra CACHE") }

    private fun oppdaterCache(ansatte: Set<NomAnsatt>) {
        if (ansatte.isEmpty()) return
        cache.putMany(NOM_CACHE, ansatte.associateBy { it.navident.verdi })
        log.trace("Oppdaterte CACHE med ${ansatte.size} ansatt(e) fra NOM")
    }

    private fun NomGraphQLRespons.toAnsatt() =
        NomAnsatt(navident, visningsnavn, gjeldendeSektor.tekst, orgTilknytninger)

    private fun NomAnsatt.kunDagligOppfolging() =
        copy(orgTilknytninger = orgTilknytninger.filterTo(mutableSetOf()) { it.erDagligOppfolging })

    private companion object {
        private val TILKNYTNINGER_QUERY = "orgtilknytninger" to "ressurs"
        private val BULK_TILKNYTNINGER_QUERY = "orgtilknytninger-bulk" to "ressurser"
        private val TILGANGSENHET_ANSATTE_QUERY = "ansatte-for-tilgangsenhet" to "ressurser"
        private val NOMID_ANSATTE_QUERY = "ansatte-for-nomid" to "orgEnheter"
        private const val NOMID_ANSATTE_TOPPNODE = "organisertUnderOrgEnhet"

        private fun nomident(nomId: String) = mapOf("organisertUnderOrgEnhetId" to nomId)
        private fun ident(ident: String) = mapOf("navident" to ident)
        private fun identer(identer: Set<String>) = mapOf("navidenter" to identer)
        private fun tilgangsenhet(enhetId: String) = mapOf("tilgangsenhetId" to enhetId)
    }
}
