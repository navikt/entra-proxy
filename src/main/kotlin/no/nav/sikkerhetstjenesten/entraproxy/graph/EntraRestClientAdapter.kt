package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponseException
import org.springframework.web.client.RestClient
import java.net.URI

@Component
class EntraRestClientAdapter(@Qualifier(GRAPH) restClient: RestClient, val cf: EntraConfig) :
    AbstractRestClientAdapter(restClient, cf) {


    fun ansattOid(navIdent: String) =
        with(get<AnsattOids>(cf.userURI(navIdent)).oids) {
            log.info("Fant $size oids i Entra for $navIdent")
            when (size) {
                0 -> throw EntraOidException(navIdent, "Fant ingen oid for navident $navIdent, er den fremdeles gyldig?")
                1 -> singleOrNull()?.id
                else -> throw EntraOidException(navIdent, "Forventet nøyaktig én oid for navident $navIdent, fant $size (${joinToString(", ") { it.id.toString() }})")
            }
        }

    fun gruppeOid(gruppeNavn: String) =
        get<Grupper>(cf.gruppeURI(gruppeNavn)).value.firstOrNull()?.id

    fun tema(ansattOid: String) =
        tilganger(cf.temaURI(ansattOid), ::Tema)

    fun enheter(ansattOid: String) =
        tilganger(cf.enheterURI(ansattOid), ::Enhetnummer)

    fun ansatteGrupper(ansattOid: String) =
        tilganger(cf.ansatteGruppeURI(ansattOid),  ::EntraGruppe )

    fun gruppeMedlemmer(gruppeOid: String) =
        pagedTransformedAndSorted(
            get<GruppeMedlemmer>(cf.gruppeMedlemmerURI(gruppeOid)),
            {
                it.next?.let(::get)
            },
            {
                it.value
            },
            {
                Ansatt(AnsattId(it.onPremisesSamAccountName),
                it.displayName, it.givenName, it.surname)
            })

    fun utvidetAnsatt(ansattId: String) =
        utvidetAnsatt(cf.navIdentURI(ansattId))

    fun utvidetAnsattTident(ansattId: String) =
        utvidetAnsatt(cf.tIdentURI(ansattId))

    private fun utvidetAnsatt(uri: URI)  =
        get<EntraSaksbehandlerRespons>(uri).ansatte.firstOrNull()

    private inline fun <T> tilganger(uri: URI, crossinline stringTransformer: (String) -> T): Set<T> where T : Comparable<T> =
        pagedTransformedAndSorted(
            get<Tilganger>(uri),
            {
                it.next?.let(::get)
            },
            {
                it.value
            },
            {
                stringTransformer(it.displayName)
            })

    private inline fun <T, V, R> pagedTransformedAndSorted(
        førsteSide: T,
        crossinline nesteSide: (T) -> T?,
        crossinline verdier: (T) -> Iterable<V>,
        noinline transform: (V) -> R): Set<R> where R : Comparable<R> =
        sorter(transformer(elementer(førsteSide, nesteSide), transform, verdier))

    private inline fun <T> elementer(førsteSide: T, crossinline nesteSide: (T) -> T?)  =
        generateSequence(førsteSide) {
            nesteSide(it)
        }

    private inline fun <T, V, R> transformer(elementer: Sequence<T>, noinline transform: (V) -> R,crossinline verdier: (T) -> Iterable<V>) =
        elementer.flatMap {
            verdier(it)
        }.map(transform)

    private fun <R : Comparable<R>> sorter(items: Sequence<R>) =
        items.toSortedSet()

    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"
}

class EntraOidException(ansattId: String, msg: String) : ErrorResponseException(NOT_FOUND) {
    init {
        body.title = TITLE
        body.detail = msg
        body.properties = mapOf("navIdent" to ansattId,"traceId" to Span.current().spanContext.traceId)
    }

    companion object   {
        const val TITLE = "Uventet respons fra Entra"
    }
}
