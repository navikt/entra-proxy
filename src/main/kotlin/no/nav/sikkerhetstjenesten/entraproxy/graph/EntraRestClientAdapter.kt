package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraRestClientAdapter.EntraGrupper.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.util.*

@Component
class EntraRestClientAdapter(@Qualifier(GRAPH) restClient: RestClient, val cf: EntraConfig) :
    AbstractRestClientAdapter(restClient, cf) {

    fun oid(ansattId: String) =
        get<EntraAnsattOidRespons>(cf.userURI(ansattId)).oids.singleOrNull()?.id

    fun gruppeId(displayName: String) =
        get<EntraGruppeRespons>(cf.gruppeURI(displayName)).value.firstOrNull()?.id

    fun tema(oid: String) =
        grupper(cf.temaURI(oid), TEMA_PREFIX, ::Tema)

    fun enheter(oid: String) =
        grupper(cf.enheterURI(oid), ENHET_PREFIX, ::Enhetnummer)

    fun medlemmer(oid: String) =
        pageTransformAndSort(
            get<EntraAnsatteRespons>(cf.medlemmerURI(oid)),
            { it.next?.let(::get) },
            { it.value },
            { AnsattId(it.navIdent) }
        )

    private inline fun <T> grupper(uri: URI, prefix: String, crossinline constructorOn: (String) -> T): Set<T> where T : Comparable<T> =
        pageTransformAndSort(
            get<EntraGrupper>(uri),
            { it.next?.let(::get) },
            { it.value },
            { constructorOn(it.displayName.removePrefix(prefix)) }
        )

    private inline fun <T, V, R> pageTransformAndSort(firstPage: T, crossinline nextPage: (T) -> T?, crossinline values: (T) -> Iterable<V>, noinline transform: (V) -> R): Set<R> where R : Comparable<R> =
        generateSequence(firstPage) { nextPage(it) }
            .flatMap { values(it) }
            .map(transform)
            .toSortedSet()


    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraAnsatteRespons(@param:JsonProperty("@odata.nextLink") val next: URI? = null, val value: Set<EntraMedlemmerAnsatt> = emptySet())

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraMedlemmerAnsatt(@param:JsonProperty("onPremisesSamAccountName") val navIdent: String)

    data class EntraGruppeRespons(@param:JsonProperty("@odata.context") val next: URI? = null, @param:JsonProperty("@odata.count") val count: Int = 0, val value: Set<EntraGruppe> = emptySet())

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraAnsattOidRespons(@param:JsonProperty("value") val oids: Set<EntraAnsattData>) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class EntraAnsattData(val id: UUID)
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraGrupper(@param:JsonProperty("@odata.nextLink") val next: URI? = null,
                                    val value: Set<EntraGruppe> = emptySet()) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class EntraGruppe(val id: UUID? = null, val displayName: String = "N/A") {
            override fun equals(other: Any?) = other is EntraGruppe && id == other.id
            override fun hashCode() = id.hashCode()
        }
    }
    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"
}
