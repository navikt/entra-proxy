package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI

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

    fun ansatteGrupper(ansattOid: String) =
        tilganger(cf.ansatteGrupperURI(ansattOid),  ::EntraGruppe )

    fun gruppeMedlemmer(gruppeOid: String): Set<Ansatt> =
        sorter(elementer(get<GruppeMedlemmer>(cf.gruppeMedlemmerURI(gruppeOid))) { it.next?.let(::get) }
                .flatMap(GruppeMedlemmer::value)
                .map { medlem -> medlem.tilAnsatt() })

    fun utvidetAnsatt(ansattId: String) =
        utvidetAnsatt(cf.navIdentURI(ansattId))

    fun utvidetAnsattTident(tIdent: String) =
        utvidetAnsatt(cf.tIdentURI(tIdent))

    private fun utvidetAnsatt(uri: URI)  =
        get<EntraSaksbehandlerRespons>(uri).ansatte.firstOrNull()

    private inline fun <T> tilganger(uri: URI, crossinline constructorOn: (String) -> T): Set<T> where T : Comparable<T> =
        pagedTransformedAndSorted(
            get<Tilganger>(uri),
            { it.next?.let(::get) },
            { it.value },
            { constructorOn(it.displayName) }
        )

    private inline fun <T, V, R> pagedTransformedAndSorted(
        førsteSide: T,
        crossinline nesteSide: (T) -> T?,
        crossinline verdier: (T) -> Iterable<V>,
        noinline transform: (V) -> R): Set<R> where R : Comparable<R> =
        sorter(transformer(elementer(førsteSide, nesteSide), verdier, transform))

    private inline fun <T> elementer(førsteSide: T, crossinline nesteSide: (T) -> T?)  =
        generateSequence(førsteSide) { nesteSide(it) }

    private inline fun <T, V, R> transformer(
        elementer: Sequence<T>,
        crossinline verdier: (T) -> Iterable<V>,
        noinline transform: (V) -> R) =
        elementer.flatMap { verdier(it) }.map(transform)

    private fun <R : Comparable<R>> sorter(items: Sequence<R>) =
        items.toSortedSet()

    fun AnsattRespons.tilAnsatt(): Ansatt =
        with(this) {
            Ansatt(
                AnsattId(onPremisesSamAccountName),
                displayName, givenName, surname)
        }
    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"
}
