package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.NAVIDENT
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.util.*

@Component
class EntraRestClientAdapter(@Qualifier(GRAPH) restClient: RestClient, val cf: EntraConfig) :
    AbstractRestClientAdapter(restClient, cf) {

    fun ansattOid(navIdent: String) =
        get<AnsattOids>(cf.userURI(navIdent)).oids.singleOrNull()?.id

    fun gruppeOid(gruppeNavn: String) =
        get<Grupper>(cf.gruppeURI(gruppeNavn)).value.firstOrNull()?.id

    fun tema(ansattOid: String) =
        tilganger(cf.temaURI(ansattOid), ::Tema)

    fun enheter(ansattOid: String) =
        tilganger(cf.enheterURI(ansattOid), ::Enhetnummer)

    fun gruppeMedlemmer(gruppeOid: String) =
        pagedTransformedAndSorted(
            get<GruppeMedlemmer>(cf.gruppeMedlemmerURI(gruppeOid)),
            { it.next?.let(::get) },
            { it.value },
            { Ansatt(AnsattId(it.navIdent), it.displayName, it.givenName,it.surname) }
        )

    fun ansatt(ansattId: String) =
         get<EntraSaksbehandlerRespons>(cf.userNavIdentURI(ansattId)).ansatte.firstOrNull()

    private inline fun <T> tilganger(uri: URI, crossinline constructorOn: (String) -> T): Set<T> where T : Comparable<T> =
        pagedTransformedAndSorted(
            get<Tilganger>(uri),
            { it.next?.let(::get) },
            { it.value },
            { constructorOn(it.displayName) }
        )

    private inline fun <T, V, R> pagedTransformedAndSorted(firstPage: T, crossinline nextPage: (T) -> T?, crossinline values: (T) -> Iterable<V>, noinline transform: (V) -> R): Set<R> where R : Comparable<R> =
        generateSequence(firstPage) { nextPage(it) }
            .flatMap { values(it) }
            .map(transform)
            .toSortedSet()


    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GruppeMedlemmer(@param:JsonProperty(NEXT_LINK) val next: URI? = null, val value: Set<GruppeMedlem> = emptySet()) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class GruppeMedlem(@param:JsonProperty(NAVIDENT) val navIdent: String,
                                val displayName: String = UKJENT,
                                val givenName: String = UKJENT,
                                val surname: String = UKJENT)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Grupper(@param:JsonProperty("@odata.context") val next: URI? = null,
                       val value: Set<IdentifiserbartObjekt> = emptySet())

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AnsattOids(@param:JsonProperty("value") val oids: Set<AnsattOid>) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class AnsattOid(val id: UUID)
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Tilganger(@param:JsonProperty(NEXT_LINK) val next: URI? = null,
                         val value: Set<IdentifiserbartObjekt> = emptySet())

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraSaksbehandlerRespons(@param:JsonProperty("value") val ansatte: Set<AnsattUtvidetInfo>)


    @JsonIgnoreProperties(ignoreUnknown = true)
    data class IdentifiserbartObjekt(val id: UUID,
                                     val displayName: String = UKJENT)

    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"

    companion object {
        private const val UKJENT = "N/A"
        private const val NEXT_LINK = "@odata.nextLink"
    }
}
