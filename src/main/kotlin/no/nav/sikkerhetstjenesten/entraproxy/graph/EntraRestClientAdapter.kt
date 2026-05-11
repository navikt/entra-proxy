package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.DefaultRestErrorHandler
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.DefaultRestErrorHandler.Companion.IDENTIFIKATOR
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.IrrecoverableRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponseException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import org.springframework.web.client.body
import java.net.URI

@Component
class EntraRestClientAdapter(
    @param:Qualifier(GRAPH) val restClient: RestClient,
    val cf: EntraConfig,
    val errorHandler: ErrorHandler = DefaultRestErrorHandler()
) : Pingable {

    val log = getLogger(javaClass)

    override fun ping() = get<Any>(cf.pingEndpoint)
    override val name = cf.name
    override val pingEndpoint = "${cf.pingEndpoint}"

    val baseURI = cf.baseUri

    fun ansattOid(navIdent: String) =
        with(get<AnsattOids>(cf.userURI(navIdent), mapOf(IDENTIFIKATOR to navIdent)).oids) {
            log.info("Fant $size oids ($this) i Entra for $navIdent")
            when (size) {
                0 -> throw NotFoundRestException(cf.userURI(navIdent), navIdent, "Fant ingen oid for navident $navIdent, er den fremdeles gyldig?")
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
        tilganger(cf.ansatteGruppeURI(ansattOid), ::EntraGruppe)

    fun gruppeMedlemmer(gruppeOid: String) =
        pagedTransformedAndSorted(
            get<GruppeMedlemmer>(cf.gruppeMedlemmerURI(gruppeOid)),
            { it.next?.let(::get) },
            { it.value },
            { Ansatt(AnsattId(it.onPremisesSamAccountName), it.displayName, it.givenName, it.surname) })

    fun utvidetAnsatt(ansattId: String) =
        utvidetAnsatt(cf.navIdentURI(ansattId), ansattId)

    fun utvidetAnsattTident(ansattId: String) =
        utvidetAnsatt(cf.tIdentURI(ansattId), ansattId)

    private fun utvidetAnsatt(uri: URI, ident: String) =
        get<EntraSaksbehandlerRespons>(uri, mapOf(IDENTIFIKATOR to ident)).ansatte.firstOrNull()

    final inline fun <reified T : Any> get(uri: URI, headers: Map<String, String> = emptyMap()) =
        restClient.get()
            .uri(uri)
            .accept(APPLICATION_JSON)
            .headers { it.setAll(headers) }
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler::handle)
            .body<T>() ?: throw IrrecoverableRestException(INTERNAL_SERVER_ERROR, uri)

    private inline fun <T> tilganger(uri: URI, crossinline stringTransformer: (String) -> T): Set<T> where T : Comparable<T> =
        pagedTransformedAndSorted(
            get<Tilganger>(uri),
            { it.next?.let(::get) },
            { it.value },
            { stringTransformer(it.displayName) })

    private inline fun <T, V, R> pagedTransformedAndSorted(
        førsteSide: T,
        crossinline nesteSide: (T) -> T?,
        crossinline verdier: (T) -> Iterable<V>,
        noinline transform: (V) -> R
    ): Set<R> where R : Comparable<R> =
        generateSequence(førsteSide) { nesteSide(it) }
            .flatMap { verdier(it) }
            .map(transform)
            .toSortedSet()

    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"
}

class EntraOidException(ansattId: String, msg: String) : ErrorResponseException(NOT_FOUND) {
    init {
        body.title = TITLE
        body.detail = msg
        body.properties = mapOf("navIdent" to ansattId, "traceId" to Span.current().spanContext.traceId)
    }

    companion object {
        const val TITLE = "Uventet respons fra Entra"
    }
}
