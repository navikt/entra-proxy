package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
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

    fun gruppeMedlemmer(gruppeOid: String) =
        pagedTransformedAndSorted(
            get<GruppeMedlemmer>(cf.gruppeMedlemmerURI(gruppeOid)),
            { it.next?.let(::get) },
            { it.value },
            { Ansatt(it.id,AnsattId(it.navIdent), it.displayName, it.givenName, it.surname) }
        )

    fun utvidetAnsatt(ansattId: String) =
        get<EntraSaksbehandlerRespons>(cf.userNavIdentURI(ansattId)).ansatte.firstOrNull()?.let {
            with(it) {
                UtvidetAnsatt(
                    id, AnsattId(onPremisesSamAccountName ), displayName,
                    givenName, surname, jobTitle, mail,officeLocation)
            }
        }

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



    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"
}

